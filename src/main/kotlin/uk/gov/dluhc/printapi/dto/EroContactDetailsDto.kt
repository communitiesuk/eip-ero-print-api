package uk.gov.dluhc.printapi.dto

data class EroContactDetailsDto(
    val name: String,
    val emailAddress: String,
    val website: String,
    val phoneNumber: String,
    var address: AddressDto,
)
