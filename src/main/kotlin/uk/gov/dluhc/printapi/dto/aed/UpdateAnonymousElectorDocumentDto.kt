package uk.gov.dluhc.printapi.dto.aed

data class UpdateAnonymousElectorDocumentDto(
    val sourceReference: String,
    val email: String?,
    val phoneNumber: String?
)
