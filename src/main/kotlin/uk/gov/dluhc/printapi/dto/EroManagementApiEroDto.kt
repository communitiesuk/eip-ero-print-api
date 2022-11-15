package uk.gov.dluhc.printapi.dto

data class EroManagementApiEroDto(
    val id: String,
    val name: String,
    val localAuthorities: List<EroManagementApiLocalAuthorityDto>,

    val englishContactDetails: EroContactDetailsDto,
    val welshContactDetails: EroContactDetailsDto? = null
)
