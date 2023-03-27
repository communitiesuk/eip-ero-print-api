package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.AnonymousElectorDocumentSummaryDto
import uk.gov.dluhc.printapi.dto.AnonymousElectorDto
import uk.gov.dluhc.printapi.dto.CertificateLanguage
import uk.gov.dluhc.printapi.dto.DeliveryAddressType
import uk.gov.dluhc.printapi.dto.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguageDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.LocalDate

fun buildAnonymousElectorDocumentSummaryDto(
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    gssCode: String = aGssCode(),
    certificateLanguage: CertificateLanguage = aValidCertificateLanguageDto(),
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    elector: AnonymousElectorDto = buildAnonymousElectorDto(),
    status: AnonymousElectorDocumentStatus = AnonymousElectorDocumentStatus.PRINTED,
    photoLocationArn: String = aPhotoArn(),
    issueDate: LocalDate = aValidIssueDate(),
    userId: String = aValidUserId(),
    requestDateTime: Instant = aValidApplicationReceivedDateTime()
): AnonymousElectorDocumentSummaryDto = AnonymousElectorDocumentSummaryDto(
    certificateNumber = certificateNumber,
    electoralRollNumber = electoralRollNumber,
    gssCode = gssCode,
    certificateLanguage = certificateLanguage,
    supportingInformationFormat = supportingInformationFormat,
    deliveryAddressType = deliveryAddressType,
    elector = elector,
    status = status,
    photoLocationArn = photoLocationArn,
    issueDate = issueDate,
    userId = userId,
    requestDateTime = requestDateTime,
)

fun buildAnonymousElectorDto(
    addressee: String = aValidDeliveryName(),
    registeredAddress: AddressDto = buildValidAddressDto()
): AnonymousElectorDto = AnonymousElectorDto(
    addressee = addressee,
    registeredAddress = registeredAddress
)
