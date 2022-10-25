package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.CertificateDelivery
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun buildCertificateDelivery(
    addressee: String = faker.name().fullName(),
    address: Address = buildAddress(),
    deliveryClass: DeliveryClass = DeliveryClass.STANDARD
) =
    CertificateDelivery(addressee = addressee, address = address, deliveryClass = deliveryClass)
