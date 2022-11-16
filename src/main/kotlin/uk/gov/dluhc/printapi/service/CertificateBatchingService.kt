package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import javax.transaction.Transactional

private val logger = KotlinLogging.logger { }

@Service
class CertificateBatchingService(
    private val idFactory: IdFactory,
    private val certificateRepository: CertificateRepository
) {

    @Transactional
    fun batchPendingCertificates(batchSize: Int): Set<String> {
        val batches = batchCertificates(batchSize)
        batches.forEach { (batchId, batchOfCertificates) ->
            batchOfCertificates.forEach { certificate ->
                certificateRepository.save(certificate)
                logger.info { "Certificate with id [${certificate.id}] assigned to batch [$batchId]" }
            }
        }
        return batches.keys
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
