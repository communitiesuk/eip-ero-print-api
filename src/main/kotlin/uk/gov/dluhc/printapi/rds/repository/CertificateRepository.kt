package uk.gov.dluhc.printapi.rds.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.rds.entity.Certificate
import java.util.UUID

@Repository
interface CertificateRepository : JpaRepository<Certificate, UUID>
