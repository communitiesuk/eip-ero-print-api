package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId

fun buildElectoralRegistrationOfficeResponse(
    id: String = aValidRandomEroId(),
    name: String = aValidEroName(),
    localAuthorities: List<LocalAuthorityResponse> = listOf(
        buildLocalAuthorityResponse(),
        buildLocalAuthorityResponse()
    ),
    roles: List<String>? = null
) = ElectoralRegistrationOfficeResponse(id = id, name = name, localAuthorities = localAuthorities, roles = roles)
