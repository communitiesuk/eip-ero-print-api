package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import javax.transaction.Transactional

private val logger = KotlinLogging.logger { }

@Service
class CertificateBatchingService(
    private val idFactory: IdFactory,
    private val certificateRepository: CertificateRepository,
    @Value("\${jobs.batch-print-requests.daily-limit}")
    private val dailyLimit: Int,
    private val clock: Clock
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

        return limitCertificates(certificatesPendingAssignment).chunked(batchSize).associate { batchOfCertificates ->
            val batchId = idFactory.batchId()
            batchId to batchOfCertificates.onEach {
                it.getCurrentPrintRequest().batchId = batchId
                it.addStatus(Status.ASSIGNED_TO_BATCH)
            }
        }
    }

    private fun limitCertificates(certificatesPendingAssignment: List<Certificate>): List<Certificate> {
        val startOfDay = Instant.now(clock).truncatedTo(DAYS)
        val endOfDay = startOfDay.plus(1, DAYS).minusSeconds(1)
        val countOfRequestsSentToPrintProvider =
            certificateRepository.getPrintRequestStatusCount(startOfDay, endOfDay, Status.ASSIGNED_TO_BATCH)

        if (certificatesPendingAssignment.size + countOfRequestsSentToPrintProvider > dailyLimit) {
            logDailyLimit(certificatesPendingAssignment, countOfRequestsSentToPrintProvider)
            return certificatesPendingAssignment.subList(0, dailyLimit - countOfRequestsSentToPrintProvider)
        }

        return certificatesPendingAssignment
    }

    private fun logDailyLimit(
        certificatesPendingAssignment: List<Certificate>,
        countOfRequestsSentToPrintProvider: Int
    ) {
        logger.warn {
            """Identified ${certificatesPendingAssignment.size} certificates to assign to a batch. 
            Daily print limit is $dailyLimit. 
            $countOfRequestsSentToPrintProvider certificates already sent to print provider today.
            Remaining capacity is ${dailyLimit - countOfRequestsSentToPrintProvider}.
            ${certificatesPendingAssignment.size + countOfRequestsSentToPrintProvider - dailyLimit} certificates won't be assigned to a batch."""
        }
    }
}
