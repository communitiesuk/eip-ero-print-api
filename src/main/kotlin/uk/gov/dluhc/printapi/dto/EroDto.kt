package uk.gov.dluhc.printapi.dto

data class EroDto(
    val eroId: String,
    val englishContactDetails: EroContactDetailsDto,
    val welshContactDetails: EroContactDetailsDto? = null
)
