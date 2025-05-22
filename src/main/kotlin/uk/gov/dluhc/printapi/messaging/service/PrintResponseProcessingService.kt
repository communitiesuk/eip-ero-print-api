package uk.gov.dluhc.printapi.messaging.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findDistinctByPrintRequestStatusAndBatchId
import uk.gov.dluhc.printapi.mapper.ProcessPrintResponseMessageMapper
import uk.gov.dluhc.printapi.mapper.StatusMapper
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.service.ElectorDocumentRemovalDateResolver
import uk.gov.dluhc.printapi.service.IdFactory
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseProcessingService(
    private val certificateRepository: CertificateRepository,
    private val idFactory: IdFactory,
    private val statusMapper: StatusMapper,
    private val processPrintResponseMessageMapper: ProcessPrintResponseMessageMapper,
    private val processPrintResponseQueue: MessageQueue<ProcessPrintResponseMessage>,
    private val certificateNotDeliveredEmailSenderService: CertificateNotDeliveredEmailSenderService,
    private val certificateFailedToPrintEmailSenderService: CertificateFailedToPrintEmailSenderService,
    private val removalDateResolver: ElectorDocumentRemovalDateResolver,
) {

    fun processPrintResponses(printResponses: List<PrintResponse>) {
        printResponses.forEach {
            processPrintResponseQueue.submit(processPrintResponseMessageMapper.toProcessPrintResponseMessage(it))
        }
    }

    /**
     * Updates print details for the batches based on the batch responses' statuses.
     * If a batch is successful, a new printRequestStatus will be added for each corresponding print details with status
     * being RECEIVED_BY_PRINT_PROVIDER.
     *
     * If a batch is not successful, the status will be set to SENT_TO_PRINT_PROVIDER and a new requestId will be set.
     * The reason for the new requestId is, the print provider does not support duplicate print requests with the same requestId.
     *
     * dateCreated will be the timestamp the new printRequestStatus is created and eventDateTime will be the timestamp from
     * the print provider's batch response.
     */
    @Transactional
    fun processBatchResponses(batchResponses: List<BatchResponse>): List<Certificate> {
        return batchResponses.flatMap { batchResponse ->
            val certificates =
                certificateRepository.findDistinctByPrintRequestStatusAndBatchId(
                    SENT_TO_PRINT_PROVIDER,
                    batchResponse.batchId
                )
            processBatchCertificates(batchResponse, certificates)
            certificateRepository.saveAll(certificates)
        }
    }

    private fun processBatchCertificates(batchResponse: BatchResponse, certificates: List<Certificate>) {
        with(batchResponse) {
            certificates.forEach {
                if (status == SUCCESS) {
                    it.addReceivedByPrintProviderEventForBatch(
                        batchId = batchId,
                        eventDateTime = timestamp.toInstant(),
                        message = message
                    )
                } else {
                    it.requeuePrintRequestForBatch(
                        batchId = batchId,
                        eventDateTime = timestamp.toInstant(),
                        message = message,
                        newRequestId = idFactory.requestId()
                    )
                }
            }
        }
    }

    @Transactional
    fun processPrintResponse(printResponse: ProcessPrintResponseMessage): Certificate? {
        val newStatus: Status

        try {
            newStatus = statusMapper.toStatusEntityEnum(printResponse.statusStep, printResponse.status)
        } catch (ex: IllegalArgumentException) {
            // TODO EROPSPT-418: Trigger alarm?
            logger.error(ex.message)
            return null
        }

        val certificate = certificateRepository.getByPrintRequestsRequestId(printResponse.requestId) ?: run {
            logger.error("Certificate not found for the requestId ${printResponse.requestId}")
            return null
        }

        with(printResponse) {
            if (newStatus == Status.PRINTED) {
                certificate.setAsPrinted(
                    requestId,
                    issueDate,
                    suggestedExpiryDate,
                )
            }

            certificate.addPrintRequestEvent(
                requestId = requestId,
                status = newStatus,
                eventDateTime = timestamp.toInstant(),
                message = message
            )
        }

        // Save and flush here to make sure that we avoid sending emails in the case of concurrency issues
        certificateRepository.saveAndFlush(certificate)

        if (printResponse.statusStep == ProcessPrintResponseMessage.StatusStep.NOT_MINUS_DELIVERED) {
            certificateNotDeliveredEmailSenderService.send(printResponse, certificate)
        } else if (printResponse.status == ProcessPrintResponseMessage.Status.FAILED) {
            certificateFailedToPrintEmailSenderService.send(printResponse, certificate)
        }

        return certificate
    }

    fun Certificate.setAsPrinted(
        requestId: String,
        newIssueDate: LocalDate?,
        newSuggestedExpiryDate: LocalDate?,
    ) {
        if (issueDate != null && suggestedExpiryDate != null) {
            return
        }

        if (newIssueDate == null || newSuggestedExpiryDate == null) {
            // TODO EROPSPT-418: Trigger alarm?
            logger.error {
                "Initial print request with requestId [${requestId}] was successfully printed, but the non-null fields issueDate and suggestedExpiryDate have values [${newIssueDate}, ${newSuggestedExpiryDate}]"
            }
            return
        }

        issueDate = newIssueDate
        suggestedExpiryDate = newSuggestedExpiryDate

        if (hasSourceApplicationBeenRemoved) {
            initialRetentionRemovalDate =
                removalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(
                    newIssueDate,
                    gssCode!!,
                )
            finalRetentionRemovalDate =
                removalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(newIssueDate)
        }
    }
}
