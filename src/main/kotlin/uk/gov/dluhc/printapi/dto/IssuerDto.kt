package uk.gov.dluhc.printapi.dto

data class IssuerDto(
    val englishContactDetails: IssuerContactDetailsDto,
    val welshContactDetails: IssuerContactDetailsDto? = null
)
