package uk.gov.dluhc.printapi.dto

data class GenerateAnonymousElectorDocumentDto(
    val gssCode: String,
    val sourceType: SourceType,
    val sourceReference: String,
    val applicationReference: String,
    val electoralRollNumber: String,
    val photoLocation: String,
    val certificateLanguage: CertificateLanguage,
    val supportingInformationFormat: SupportingInformationFormat?,
    val firstName: String,
    val middleNames: String? = null,
    val surname: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val address: AddressDto,
    val userId: String,
)
