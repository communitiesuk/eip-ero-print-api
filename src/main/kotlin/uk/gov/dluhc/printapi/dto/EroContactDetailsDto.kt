package uk.gov.dluhc.printapi.dto

data class EroContactDetailsDto(
    val name: String,
    val nameVac: String,
    val emailAddress: String,
    val emailAddressVac: String?,
    val website: String,
    val websiteVac: String,
    val phoneNumber: String,
    var address: AddressDto,
)
