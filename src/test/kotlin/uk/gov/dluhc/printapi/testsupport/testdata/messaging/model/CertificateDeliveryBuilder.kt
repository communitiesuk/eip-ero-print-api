package uk.gov.dluhc.printapi.testsupport.testdata.messaging.model

import uk.gov.dluhc.printapi.messaging.models.Address
import uk.gov.dluhc.printapi.messaging.models.AddressFormat
import uk.gov.dluhc.printapi.messaging.models.CertificateDelivery
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType
import uk.gov.dluhc.printapi.messaging.models.DeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun buildMessagingCertificateDelivery(
    addressee: String = faker.name().fullName(),
    address: Address = buildAddress(),
    deliveryClass: DeliveryClass = DeliveryClass.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    addressFormat: AddressFormat = AddressFormat.UK,
): CertificateDelivery = CertificateDelivery(
    addressee = addressee,
    address = address,
    deliveryClass = deliveryClass,
    deliveryAddressType = deliveryAddressType,
    addressFormat = addressFormat,
)
