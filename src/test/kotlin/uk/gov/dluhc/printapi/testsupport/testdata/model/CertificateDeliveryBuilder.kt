package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.Address
import uk.gov.dluhc.printapi.messaging.models.AddressFormat
import uk.gov.dluhc.printapi.messaging.models.CertificateDelivery
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType
import uk.gov.dluhc.printapi.messaging.models.DeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun buildCertificateDelivery(
    addressee: String = faker.name().fullName(),
    address: Address = buildAddress(),
    deliveryClass: DeliveryClass = DeliveryClass.STANDARD,
    addressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    addressFormat: AddressFormat = AddressFormat.UK,
): CertificateDelivery = CertificateDelivery(
    addressee = addressee,
    address = address,
    deliveryClass = deliveryClass,
    addressType = addressType,
    addressFormat = addressFormat,
)
