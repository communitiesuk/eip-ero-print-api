package uk.gov.dluhc.printapi.testsupport.testdata.dto.aed

import uk.gov.dluhc.printapi.dto.DeliveryAddressType
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId

fun buildReIssueAnonymousElectorDocumentDto(
    sourceReference: String = aValidSourceReference(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    userId: String = aValidUserId(),
): ReIssueAnonymousElectorDocumentDto =
    ReIssueAnonymousElectorDocumentDto(
        sourceReference = sourceReference,
        electoralRollNumber = electoralRollNumber,
        deliveryAddressType = deliveryAddressType,
        userId = userId
    )
