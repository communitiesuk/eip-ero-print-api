package uk.gov.dluhc.printapi.testsupport.testdata.messaging.model

import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import uk.gov.dluhc.printapi.messaging.models.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode

fun buildApplicationRemovedMessage(
    sourceReference: String = aValidSourceReference(),
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    gssCode: String = getRandomGssCode(),
) = ApplicationRemovedMessage(
    sourceReference = sourceReference,
    sourceType = sourceType,
    gssCode = gssCode
)
