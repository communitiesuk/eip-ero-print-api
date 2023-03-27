package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.Address
import uk.gov.dluhc.printapi.models.AnonymousElector
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.models.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.LocalDate
import java.time.OffsetDateTime

fun buildAnonymousElectorDocumentSummary(
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    gssCode: String = aGssCode(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    elector: AnonymousElector = buildAnonymousElector(),
    status: AnonymousElectorDocumentStatus = AnonymousElectorDocumentStatus.PRINTED,
    photoLocation: String = aPhotoArn(),
    issueDate: LocalDate = aValidIssueDate(),
    userId: String = aValidUserId(),
    dateTime: OffsetDateTime = aValidGeneratedDateTime()
): AnonymousElectorDocumentSummary = AnonymousElectorDocumentSummary(
    certificateNumber = certificateNumber,
    electoralRollNumber = electoralRollNumber,
    gssCode = gssCode,
    certificateLanguage = certificateLanguage,
    supportingInformationFormat = supportingInformationFormat,
    deliveryAddressType = deliveryAddressType,
    elector = elector,
    status = status,
    photoLocation = photoLocation,
    issueDate = issueDate,
    userId = userId,
    dateTime = dateTime,
)

fun buildAnonymousElector(
    addressee: String = aValidDeliveryName(),
    registeredAddress: Address = buildValidAddress(),
): AnonymousElector = AnonymousElector(
    addressee = addressee,
    registeredAddress = registeredAddress
)
