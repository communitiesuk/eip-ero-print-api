package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository

private val logger = KotlinLogging.logger { }

@Component
class PrintRequestsService(
    private val printDetailsRepository: PrintDetailsRepository,
    private val certificateRepository: CertificateRepository,
    private val idFactory: IdFactory,
    private val processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>,
) {

    fun processPrintRequests(batchSize: Int) {
        processDynamoBatch(batchSize)
        processBatch(batchSize)
    }

    private fun processBatch(batchSize: Int) {
        batchCertificates(batchSize).forEach { (batchId, batchOfCertificates) ->
            batchOfCertificates.forEach { certificate ->
                certificateRepository.save(certificate)
                logger.info { "Certificate with id [${certificate.id}] assigned to batch [$batchId]" }
            }
            processPrintRequestQueue.submit(ProcessPrintRequestBatchMessage(batchId))
            logger.info { "Batch [$batchId] submitted to queue" }
        }
    }

    fun batchCertificates(batchSize: Int): Map<String, List<Certificate>> {
        val certificatesPendingAssignment = certificateRepository.findByStatusIs(Status.PENDING_ASSIGNMENT_TO_BATCH)
        return certificatesPendingAssignment.chunked(batchSize).associate { batchOfCertificates ->
            val batchId = idFactory.batchId()
            batchId to batchOfCertificates.onEach {
                it.getCurrentPrintRequest().batchId = batchId
                it.addStatus(Status.ASSIGNED_TO_BATCH)
            }
        }
    }

    private fun processDynamoBatch(batchSize: Int) {
        batchPrintRequests(batchSize).forEach { (batchId, batchOfPrintDetails) ->
            batchOfPrintDetails.forEach { pd ->
                printDetailsRepository.save(pd)
                logger.info { "Print request with id [${pd.id}] assigned to batch [${pd.batchId}]" }
            }
            processPrintRequestQueue.submit(ProcessPrintRequestBatchMessage(batchId))
            logger.info { "Batch [$batchId] submitted to queue" }
        }
    }

    fun batchPrintRequests(batchSize: Int): Map<String, List<PrintDetails>> {
        val printDetailsPendingAssignment = printDetailsRepository.getAllByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        return printDetailsPendingAssignment.chunked(batchSize).associate { batchOfPrintDetails ->
            val batchId = idFactory.batchId()
            batchId to batchOfPrintDetails.onEach {
                it.batchId = batchId
                it.addStatus(Status.ASSIGNED_TO_BATCH)
            }
        }
    }
}
