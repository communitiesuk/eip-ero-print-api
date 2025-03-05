package uk.gov.dluhc.printapi.config

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentDelivery
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import java.util.UUID

@Repository
interface TestPrintRequestRepository : JpaRepository<PrintRequest, UUID>

@Repository
interface TestAddressRepository : JpaRepository<Address, UUID>

@Repository
interface TestDeliveryRepository : JpaRepository<Delivery, UUID>

@Repository
interface TestAnonymousElectorDocumentDeliveryRepository : JpaRepository<AnonymousElectorDocumentDelivery, UUID>
