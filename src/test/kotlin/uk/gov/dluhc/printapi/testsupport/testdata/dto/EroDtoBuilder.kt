package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto

fun buildEroDto(
    englishContactDetails: EroContactDetailsDto = anEnglishEroContactDetails(),
    welshContactDetails: EroContactDetailsDto? = aWelshEroContactDetails()
) = EroDto(
    englishContactDetails = englishContactDetails,
    welshContactDetails = welshContactDetails,
)
