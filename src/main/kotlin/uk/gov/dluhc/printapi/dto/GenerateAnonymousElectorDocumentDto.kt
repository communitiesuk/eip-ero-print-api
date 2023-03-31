package uk.gov.dluhc.printapi.dto

data class GenerateAnonymousElectorDocumentDto(
    val gssCode: String,
    val sourceType: SourceType,
    val sourceReference: String,
    val applicationReference: String,
    val electoralRollNumber: String,
    val photoLocation: String,
    val certificateLanguage: CertificateLanguage,
    val supportingInformationFormat: AnonymousSupportingInformationFormat,
    val firstName: String,
    val middleNames: String? = null,
    val surname: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val registeredAddress: AddressDto,
    val delivery: CertificateDelivery,
    val userId: String,
)
