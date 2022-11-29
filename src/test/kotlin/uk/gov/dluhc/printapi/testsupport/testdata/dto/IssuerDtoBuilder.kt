package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.IssuerContactDetailsDto
import uk.gov.dluhc.printapi.dto.IssuerDto

fun buildIssuerDto(
    englishContactDetails: IssuerContactDetailsDto = anEnglishIssuerContactDetails(),
    welshContactDetails: IssuerContactDetailsDto? = aWelshIssuerContactDetails()
) = IssuerDto(
    englishContactDetails = englishContactDetails,
    welshContactDetails = welshContactDetails,
)
