package uk.gov.dluhc.printapi.dto

data class EroDto(
    val englishContactDetails: EroContactDetailsDto,
    val welshContactDetails: EroContactDetailsDto? = null
)
