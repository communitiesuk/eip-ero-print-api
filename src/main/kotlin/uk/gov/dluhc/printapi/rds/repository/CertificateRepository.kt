package uk.gov.dluhc.printapi.rds.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.rds.entity.Certificate
import java.util.UUID

@Repository
interface CertificateRepository : JpaRepository<Certificate, UUID> {

    fun getByPrintRequestsRequestId(requestId: String): Certificate?

    fun findByStatusAndPrintRequestsBatchId(certificateStatus: Status, batchId: String): List<Certificate>

    fun findByStatus(certificateStatus: Status): List<Certificate>
}
