package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.UpdateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference

fun buildUpdateAnonymousElectorDocumentRequest(
    sourceReference: String = aValidSourceReference(),
    email: String? = aValidEmailAddress(),
    phoneNumber: String? = aValidPhoneNumber()
): UpdateAnonymousElectorDocumentRequest =
    UpdateAnonymousElectorDocumentRequest(
        sourceReference = sourceReference,
        email = email,
        phoneNumber = phoneNumber,
    )
