package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.Address
import uk.gov.dluhc.printapi.models.AddressFormat
import uk.gov.dluhc.printapi.models.CertificateDelivery
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.models.DeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun buildApiCertificateDelivery(
    addressee: String = faker.name().fullName(),
    deliveryAddress: Address = buildValidAddress(),
    deliveryClass: DeliveryClass = DeliveryClass.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    collectionReason: String? = null,
    addressFormat: AddressFormat = AddressFormat.UK,
): CertificateDelivery = CertificateDelivery(
    addressee = addressee,
    deliveryAddress = deliveryAddress,
    deliveryClass = deliveryClass,
    deliveryAddressType = deliveryAddressType,
    collectionReason = collectionReason,
    addressFormat = addressFormat,
)
