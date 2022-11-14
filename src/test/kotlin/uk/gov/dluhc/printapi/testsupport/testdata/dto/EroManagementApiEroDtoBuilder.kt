package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.dto.EroManagementApiLocalAuthorityDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId

fun buildEroManagementApiEroDto(
    id: String = aValidRandomEroId(),
    name: String = aValidEroName(),
    localAuthorities: List<EroManagementApiLocalAuthorityDto> = listOf(
        buildEroManagementApiLocalAuthorityDto(),
        buildEroManagementApiLocalAuthorityDto()
    ),
    englishContactDetails: EroContactDetailsDto = anEnglishEroContactDetails(),
    welshContactDetails: EroContactDetailsDto? = aWelshEroContactDetails()
) = EroManagementApiEroDto(
    id = id,
    name = name,
    localAuthorities = localAuthorities,
    englishContactDetails = englishContactDetails,
    welshContactDetails = welshContactDetails,
)
