package uk.gov.dluhc.printapi.dto.aed

import uk.gov.dluhc.printapi.dto.DeliveryAddressType

data class ReIssueAnonymousElectorDocumentDto(
    val sourceReference: String,
    val electoralRollNumber: String,
    val deliveryAddressType: DeliveryAddressType,
    val userId: String,
)
