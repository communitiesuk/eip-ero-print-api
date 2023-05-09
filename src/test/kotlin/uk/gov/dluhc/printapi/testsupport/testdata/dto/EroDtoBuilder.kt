package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId

fun buildEroDto(
    eroId: String = aValidRandomEroId(),
    englishContactDetails: EroContactDetailsDto = anEnglishEroContactDetails(),
    welshContactDetails: EroContactDetailsDto? = aWelshEroContactDetails()
) = EroDto(
    eroId = eroId,
    englishContactDetails = englishContactDetails,
    welshContactDetails = welshContactDetails,
)
