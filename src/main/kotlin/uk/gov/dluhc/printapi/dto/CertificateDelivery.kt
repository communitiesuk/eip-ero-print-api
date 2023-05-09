package uk.gov.dluhc.printapi.dto

data class CertificateDelivery(
    val deliveryClass: DeliveryClass,
    val deliveryAddressType: DeliveryAddressType,
    val collectionReason: String?,
    val addressee: String,
    val deliveryAddress: AddressDto,
    val addressFormat: AddressFormat,
)

enum class DeliveryClass {
    STANDARD,
}

enum class DeliveryAddressType {
    REGISTERED,
    ERO_COLLECTION,
    ALTERNATIVE
}

enum class AddressFormat {
    UK,
    OVERSEAS,
    BFPO
}
