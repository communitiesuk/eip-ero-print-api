package uk.gov.dluhc.printapi.dto.aed

import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.CertificateLanguage
import uk.gov.dluhc.printapi.dto.DeliveryAddressType
import java.time.Instant
import java.time.LocalDate

class AnonymousElectorDocumentDto(
    val certificateNumber: String,
    val electoralRollNumber: String,
    val gssCode: String,
    val certificateLanguage: CertificateLanguage,
    val supportingInformationFormat: AnonymousSupportingInformationFormat,
    val deliveryAddressType: DeliveryAddressType,
    val elector: AnonymousElectorDto,
    val status: AnonymousElectorDocumentStatus,
    val photoLocationArn: String,
    val issueDate: LocalDate,
    val userId: String,
    val requestDateTime: Instant,
)

class AnonymousElectorDto(
    val firstName: String,
    val middleNames: String?,
    val surname: String,
    val addressee: String,
    val registeredAddress: AddressDto,
    val email: String?,
    val phoneNumber: String?
)

enum class AnonymousElectorDocumentStatus {
    GENERATED,
    PRINTED,
}

enum class AnonymousSupportingInformationFormat {
    STANDARD,
    BRAILLE,
    LARGE_PRINT,
    EASY_READ,
}
