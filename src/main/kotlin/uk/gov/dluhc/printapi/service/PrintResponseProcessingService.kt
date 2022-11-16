package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.ProcessPrintResponseMessageMapper
import uk.gov.dluhc.printapi.mapper.StatusMapper
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import javax.transaction.Transactional

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseProcessingService(
    private val certificateRepository: CertificateRepository,
    private val idFactory: IdFactory,
    private val statusMapper: StatusMapper,
    private val processPrintResponseMessageMapper: ProcessPrintResponseMessageMapper,
    private val processPrintResponseQueue: MessageQueue<ProcessPrintResponseMessage>
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
    fun processBatchResponses(batchResponses: List<BatchResponse>) {
        batchResponses.forEach { batchResponse ->
            val certificates =
                certificateRepository.findByStatusAndPrintRequestsBatchId(
                    SENT_TO_PRINT_PROVIDER,
                    batchResponse.batchId
                )
            processBatchCertificates(batchResponse, certificates)
            certificateRepository.saveAll(certificates)
        }
    }

    private fun processBatchCertificates(batchResponse: BatchResponse, certificates: List<Certificate>) {
        with(batchResponse) {
            val newStatus =
                if (status == SUCCESS) RECEIVED_BY_PRINT_PROVIDER else PENDING_ASSIGNMENT_TO_BATCH

            certificates.forEach {
                it.addStatus(
                    status = newStatus,
                    eventDateTime = timestamp.toInstant(),
                    message = message
                )

                if (status != SUCCESS) {
                    val currentPrintRequest = it.getCurrentPrintRequest()
                    currentPrintRequest.requestId = idFactory.requestId()
                    currentPrintRequest.batchId = null
                }
            }
        }
    }

    @Transactional
    fun processPrintResponse(printResponse: ProcessPrintResponseMessage) {
        val newStatus: Status

        try {
            newStatus = statusMapper.toStatusEntityEnum(printResponse.statusStep, printResponse.status)
        } catch (ex: IllegalArgumentException) {
            logger.error(ex.message)
            return
        }

        val certificate = certificateRepository.getByPrintRequestsRequestId(printResponse.requestId)

        if (certificate == null) {
            logger.error("Certificate not found for the requestId ${printResponse.requestId}")
            return
        }

        with(printResponse) {
            certificate.addStatus(
                status = newStatus,
                eventDateTime = timestamp.toInstant(),
                message = message
            )
        }

        certificateRepository.save(certificate)
    }
}
