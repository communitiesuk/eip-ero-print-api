package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.Address
import uk.gov.dluhc.printapi.messaging.models.CertificateDelivery
import uk.gov.dluhc.printapi.messaging.models.DeliveryClass
import uk.gov.dluhc.printapi.messaging.models.DeliveryMethod
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun buildCertificateDelivery(
    addressee: String = faker.name().fullName(),
    address: Address = buildAddress(),
    deliveryClass: DeliveryClass = DeliveryClass.STANDARD,
    deliveryMethod: DeliveryMethod = DeliveryMethod.DELIVERY
) =
    CertificateDelivery(addressee = addressee, address = address, deliveryClass = deliveryClass, deliveryMethod = deliveryMethod)
