package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Delivery
import java.util.UUID

/**
 * Repository class for [Delivery] entities. This has been added to aid the removal of [Delivery] entities as part of
 * the initial retention period data. Otherwise, it should only be necessary to use the top level
 * [CertificateRepository].
 */
@Repository
interface DeliveryRepository : JpaRepository<Delivery, UUID>
