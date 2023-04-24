package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.models.ReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference

fun buildReIssueAnonymousElectorDocumentRequest(
    sourceReference: String = aValidSourceReference(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
): ReIssueAnonymousElectorDocumentRequest =
    ReIssueAnonymousElectorDocumentRequest(
        sourceReference = sourceReference,
        electoralRollNumber = electoralRollNumber,
        deliveryAddressType = deliveryAddressType,
    )
