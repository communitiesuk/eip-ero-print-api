package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import uk.gov.dluhc.printapi.messaging.models.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode

fun buildApplicationRemovedMessage(
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    gssCode: String = getRandomGssCode(),
) = ApplicationRemovedMessage(
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    sourceType = sourceType,
    gssCode = gssCode
)
