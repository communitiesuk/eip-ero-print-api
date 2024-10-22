package uk.gov.dluhc.printapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS

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
    fun batchPendingCertificates(): List<BatchInfo> {
        val batches = batchCertificates()
        batches.forEach { (batchId, batchOfCertificates) ->
            certificateRepository.saveAll(batchOfCertificates)
            logger.info { "Certificate ids ${batchOfCertificates.map { it.id }} assigned to batch [$batchId]" }
        }
        return batches.map { (batchId, certificates) ->
            BatchInfo(batchId, countPrintRequestsAssignedToBatch(certificates, batchId))
        }
    }

    fun batchCertificates(): Map<String, List<Certificate>> {
        val certificatesPendingAssignment = certificateRepository.findByStatusOrderByApplicationReceivedDateTimeAsc(
            PENDING_ASSIGNMENT_TO_BATCH,
            Pageable.ofSize(maxUnBatchedRecords)
        )

        val printRequestsPendingAssignment = certificatesPendingAssignment.flatMap { it.printRequests }
            .filter { it.getCurrentStatus().status == PENDING_ASSIGNMENT_TO_BATCH }

        return limitPrintRequests(printRequestsPendingAssignment).chunked(batchSize).associate { batchOfPrintRequests ->
            val batchId = idFactory.batchId()
            batchId to batchOfPrintRequests.map { printRequest ->
                val certificate = certificatesPendingAssignment.first { it.printRequests.contains(printRequest) }
                certificate.addPrintRequestToBatch(printRequest, batchId)
                certificate
            }
        }
    }

    private fun limitPrintRequests(printRequestsPendingAssignmentToBatch: List<PrintRequest>): List<PrintRequest> {
        val startOfDay = Instant.now(clock).truncatedTo(DAYS)
        val endOfDay = startOfDay.plus(1, DAYS).minusSeconds(1)
        val countOfRequestsSentToPrintProvider =
            certificateRepository.getPrintRequestStatusCount(startOfDay, endOfDay, ASSIGNED_TO_BATCH)

        if ((printRequestsPendingAssignmentToBatch.size + countOfRequestsSentToPrintProvider) > dailyLimit) {
            logDailyLimit(printRequestsPendingAssignmentToBatch, countOfRequestsSentToPrintProvider, endOfDay)
            return printRequestsPendingAssignmentToBatch.subList(0, dailyLimit - countOfRequestsSentToPrintProvider)
        }

        return printRequestsPendingAssignmentToBatch
    }

    private fun logDailyLimit(
        printRequestsPendingAssignmentToBatch: List<PrintRequest>,
        countOfRequestsSentToPrintProvider: Int,
        endOfDay: Instant
    ) {
        val nextDay = endOfDay.plusSeconds(1)
        logger.warn {
            """Identified ${printRequestsPendingAssignmentToBatch.size} print requests to assign to a batch. 
            Daily print limit is $dailyLimit. 
            $countOfRequestsSentToPrintProvider print requests already sent to print provider today.
            Remaining capacity is ${dailyLimit - countOfRequestsSentToPrintProvider}.
            ${printRequestsPendingAssignmentToBatch.size + countOfRequestsSentToPrintProvider - dailyLimit} print requests won't be assigned to a batch until $nextDay at the earliest."""
        }
    }
}

data class BatchInfo(
    val batchId: String,
    val printRequestCount: Int,
)
