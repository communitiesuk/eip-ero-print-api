package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import javax.transaction.Transactional

private val logger = KotlinLogging.logger { }

@Component
class PrintRequestsService(
    private val certificateRepository: CertificateRepository,
    private val idFactory: IdFactory,
    private val processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>,
) {

    @Transactional
    fun processPrintRequests(batchSize: Int) {
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
        val certificatesPendingAssignment = certificateRepository.findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        return certificatesPendingAssignment.chunked(batchSize).associate { batchOfCertificates ->
            val batchId = idFactory.batchId()
            batchId to batchOfCertificates.onEach {
                it.getCurrentPrintRequest().batchId = batchId
                it.addStatus(Status.ASSIGNED_TO_BATCH)
            }
        }
    }
}
