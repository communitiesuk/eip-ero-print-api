package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.EroManagementApiLocalAuthorityDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode

fun buildEroManagementApiLocalAuthorityDto(
    gssCode: String = getRandomGssCode(),
    name: String = aValidLocalAuthorityName()
) = EroManagementApiLocalAuthorityDto(
    gssCode = gssCode,
    name = name
)
