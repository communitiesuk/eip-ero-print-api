package uk.gov.dluhc.printapi.dto

import java.time.Instant
import java.time.LocalDate

class AnonymousElectorDocumentSummaryDto(
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
    val addressee: String,
    val registeredAddress: AddressDto,
)

enum class AnonymousElectorDocumentStatus {
    GENERATED,
    PRINTED,
}
