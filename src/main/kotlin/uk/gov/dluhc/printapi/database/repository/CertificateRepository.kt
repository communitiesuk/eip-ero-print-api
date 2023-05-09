package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SourceType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Repository
interface CertificateRepository : JpaRepository<Certificate, UUID> {

    fun getByPrintRequestsRequestId(requestId: String): Certificate?

    fun findByPrintRequestsBatchId(batchId: String): List<Certificate>

    fun findByStatusOrderByApplicationReceivedDateTimeAsc(certificateStatus: Status, pageable: Pageable): List<Certificate>

    fun findByGssCodeAndSourceTypeAndSourceReference(gssCode: String, sourceType: SourceType, sourceReference: String): Certificate?

    fun findByGssCodeInAndSourceTypeAndSourceReference(gssCodes: List<String>, sourceType: SourceType, sourceReference: String): Certificate?

    fun findBySourceTypeAndInitialRetentionDataRemovedAndInitialRetentionRemovalDateBefore(
        sourceType: SourceType,
        initialRetentionDataRemoved: Boolean = false,
        initialRetentionRemovalDate: LocalDate
    ): List<Certificate>

    fun findBySourceTypeAndFinalRetentionRemovalDateBefore(
        sourceType: SourceType,
        finalRetentionRemovalDate: LocalDate,
        batchRequest: Pageable
    ): Page<CertificateRemovalSummary>

    fun countBySourceTypeAndFinalRetentionRemovalDateBefore(
        sourceType: SourceType,
        finalRetentionRemovalDate: LocalDate = LocalDate.now(),
    ): Int

    @Query(
        value = """
            SELECT COUNT(s) FROM PrintRequestStatus s
            WHERE s.eventDateTime >= :startInstant 
            AND s.eventDateTime <= :endInstant 
            AND s.status = :status
        """
    )
    fun getPrintRequestStatusCount(startInstant: Instant, endInstant: Instant, status: Status): Int
}

object CertificateRepositoryExtensions {
    fun CertificateRepository.findDistinctByPrintRequestStatusAndBatchId(status: Status, batchId: String): List<Certificate> {
        return findByPrintRequestsBatchId(batchId).toSet()
            .filter { it.printRequests.any { printRequest -> printRequest.getCurrentStatus().status == status } }
    }

    fun CertificateRepository.findPendingRemovalOfInitialRetentionData(sourceType: SourceType): List<Certificate> {
        return findBySourceTypeAndInitialRetentionDataRemovedAndInitialRetentionRemovalDateBefore(
            sourceType = sourceType,
            initialRetentionRemovalDate = LocalDate.now()
        )
    }

    fun CertificateRepository.findPendingRemovalOfFinalRetentionData(
        sourceType: SourceType,
        batchNumber: Int,
        batchSize: Int = 10000
    ): Page<CertificateRemovalSummary> {
        val batchRequest = PageRequest.of(
            batchNumber - 1,
            batchSize,
            Sort.by(Sort.Direction.ASC, "sourceType", "finalRetentionRemovalDate", "id")
        )

        return findBySourceTypeAndFinalRetentionRemovalDateBefore(
            sourceType = sourceType,
            finalRetentionRemovalDate = LocalDate.now(),
            batchRequest = batchRequest
        )
    }
}

/**
 * Used as a JPA class based projection to retrieve the minimum set of data to remove a Certificate and its photo, via
 * an SQS queue.
 */
data class CertificateRemovalSummary(
    val id: UUID? = null,
    val photoLocationArn: String?
)
