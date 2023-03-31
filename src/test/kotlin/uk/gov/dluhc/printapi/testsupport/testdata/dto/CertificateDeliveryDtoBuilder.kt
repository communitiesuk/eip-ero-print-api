package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.AddressFormat
import uk.gov.dluhc.printapi.dto.CertificateDelivery
import uk.gov.dluhc.printapi.dto.DeliveryAddressType
import uk.gov.dluhc.printapi.dto.DeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildValidAddressDto

fun buildDtoCertificateDelivery(
    addressee: String = DataFaker.faker.name().fullName(),
    deliveryAddress: AddressDto = buildValidAddressDto(),
    deliveryClass: DeliveryClass = DeliveryClass.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    addressFormat: AddressFormat = AddressFormat.UK,
): CertificateDelivery = CertificateDelivery(
    addressee = addressee,
    deliveryAddress = deliveryAddress,
    deliveryClass = deliveryClass,
    deliveryAddressType = deliveryAddressType,
    addressFormat = addressFormat,
)
