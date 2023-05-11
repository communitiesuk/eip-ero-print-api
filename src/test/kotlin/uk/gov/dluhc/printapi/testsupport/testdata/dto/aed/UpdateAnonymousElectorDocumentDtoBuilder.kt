package uk.gov.dluhc.printapi.testsupport.testdata.dto.aed

import uk.gov.dluhc.printapi.dto.aed.UpdateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference

fun buildUpdateAnonymousElectorDocumentDto(
    sourceReference: String = aValidSourceReference(),
    email: String? = aValidEmailAddress(),
    phoneNumber: String? = aValidPhoneNumber()
): UpdateAnonymousElectorDocumentDto =
    UpdateAnonymousElectorDocumentDto(
        sourceReference = sourceReference,
        email = email,
        phoneNumber = phoneNumber,
    )
