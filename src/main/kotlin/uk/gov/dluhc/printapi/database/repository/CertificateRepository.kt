package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import java.time.Instant
import java.util.UUID

@Repository
interface CertificateRepository : JpaRepository<Certificate, UUID> {

    fun getByPrintRequestsRequestId(requestId: String): Certificate?

    fun findByPrintRequestsBatchId(batchId: String): List<Certificate>

    fun findByStatusOrderByApplicationReceivedDateTimeAsc(certificateStatus: Status, pageable: Pageable): List<Certificate>

    fun findByGssCodeInAndSourceTypeAndSourceReference(gssCodes: List<String>, sourceType: SourceType, sourceReference: String): Certificate?

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
    fun CertificateRepository.findByPrintRequestStatusAndBatchId(status: Status, batchId: String): List<Certificate> {
        return findByPrintRequestsBatchId(batchId)
            .filter { it.printRequests.stream().anyMatch { printRequest -> printRequest.getCurrentStatus().status == status } }
    }
}
