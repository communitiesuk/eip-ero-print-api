package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode

fun buildLocalAuthorityResponse(
    gssCode: String = getRandomGssCode(),
    name: String = aValidLocalAuthorityName()
) = LocalAuthorityResponse(gssCode = gssCode, name = name)
