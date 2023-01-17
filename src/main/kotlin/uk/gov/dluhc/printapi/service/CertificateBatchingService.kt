package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
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
    @Value("\${jobs.batch-print-requests.daily-limit}") private val dailyLimit: Int,
    @Value("\${jobs.batch-print-requests.batch-size}") private val batchSize: Int,
    @Value("\${jobs.batch-print-requests.max-un-batched-records}") private val maxUnBatchedRecords: Int,
    private val clock: Clock
) {
    @Transactional
    fun batchPendingCertificates(): Set<String> {
        val batches = batchCertificates()
        batches.forEach { (batchId, batchOfCertificates) ->
            certificateRepository.saveAll(batchOfCertificates)
            logger.info { "Certificate ids ${batchOfCertificates.map { it.id }} assigned to batch [$batchId]" }
        }
        return batches.keys
    }

    fun batchCertificates(): Map<String, List<Certificate>> {
        val certificatesPendingAssignment = certificateRepository.findByStatusOrderByApplicationReceivedDateTimeAsc(
            PENDING_ASSIGNMENT_TO_BATCH,
            Pageable.ofSize(maxUnBatchedRecords)
        )

        return limitCertificates(certificatesPendingAssignment).chunked(batchSize).associate { batchOfCertificates ->
            val batchId = idFactory.batchId()
            batchId to batchOfCertificates.onEach { it.addPrintRequestToBatch(batchId) }
        }
    }

    private fun limitCertificates(certificatesPendingAssignment: List<Certificate>): List<Certificate> {
        val startOfDay = Instant.now(clock).truncatedTo(DAYS)
        val endOfDay = startOfDay.plus(1, DAYS).minusSeconds(1)
        val countOfRequestsSentToPrintProvider =
            certificateRepository.getPrintRequestStatusCount(startOfDay, endOfDay, ASSIGNED_TO_BATCH)

        if ((certificatesPendingAssignment.size + countOfRequestsSentToPrintProvider) > dailyLimit) {
            logDailyLimit(certificatesPendingAssignment, countOfRequestsSentToPrintProvider, endOfDay)
            return certificatesPendingAssignment.subList(0, dailyLimit - countOfRequestsSentToPrintProvider)
        }

        return certificatesPendingAssignment
    }

    private fun logDailyLimit(
        certificatesPendingAssignment: List<Certificate>,
        countOfRequestsSentToPrintProvider: Int,
        endOfDay: Instant
    ) {
        val nextDay = endOfDay.plusSeconds(1)
        logger.warn {
            """Identified ${certificatesPendingAssignment.size} certificates to assign to a batch. 
            Daily print limit is $dailyLimit. 
            $countOfRequestsSentToPrintProvider certificates already sent to print provider today.
            Remaining capacity is ${dailyLimit - countOfRequestsSentToPrintProvider}.
            ${certificatesPendingAssignment.size + countOfRequestsSentToPrintProvider - dailyLimit} certificates won't be assigned to a batch until $nextDay at the earliest."""
        }
    }
}
