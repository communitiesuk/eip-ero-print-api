package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Address
import java.util.UUID

/**
 * Repository class for [Address] entities. This has been added to aid the removal of [Address] entities as part of
 * the initial retention period data for Anonymous Elector Documents. Otherwise, it should only be necessary to use the
 * top level [AnonymousElectorDocumentRepository].
 */
@Repository
interface AddressRepository : JpaRepository<Address, UUID>
