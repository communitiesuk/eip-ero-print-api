package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import java.time.Instant
import java.util.UUID

@Repository
interface CertificateRepository : JpaRepository<Certificate, UUID> {

    fun getByPrintRequestsRequestId(requestId: String): Certificate?

    fun findByStatusAndPrintRequestsBatchId(certificateStatus: Status, batchId: String): List<Certificate>

    fun findByStatus(certificateStatus: Status): List<Certificate>

    fun findByGssCodeInAndSourceTypeAndSourceReference(gssCodes: List<String>, sourceType: SourceType, sourceReference: String): Certificate?

    fun findByPrintRequestsStatusHistoryEventDateTimeBetweenAndPrintRequestsStatusHistoryStatus(
        startInstant: Instant,
        endInstant: Instant,
        status: Status
    ): List<Certificate>
}
