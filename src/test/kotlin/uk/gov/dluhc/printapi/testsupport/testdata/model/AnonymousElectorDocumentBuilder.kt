package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.Address
import uk.gov.dluhc.printapi.models.AnonymousElector
import uk.gov.dluhc.printapi.models.AnonymousElectorDocument
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat
import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.zip.anAedPhotoUrl
import java.time.LocalDate
import java.time.OffsetDateTime

fun buildAnonymousElectorDocumentApi(
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    gssCode: String = aGssCode(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: AnonymousSupportingInformationFormat = AnonymousSupportingInformationFormat.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    collectionReason: String? = null,
    elector: AnonymousElector = buildAnonymousElectorApi(),
    status: AnonymousElectorDocumentStatus = AnonymousElectorDocumentStatus.PRINTED,
    photoUrl: String = anAedPhotoUrl(),
    issueDate: LocalDate = aValidIssueDate(),
    userId: String = aValidUserId(),
    dateTime: OffsetDateTime = aValidGeneratedDateTime()
): AnonymousElectorDocument = AnonymousElectorDocument(
    certificateNumber = certificateNumber,
    electoralRollNumber = electoralRollNumber,
    gssCode = gssCode,
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    certificateLanguage = certificateLanguage,
    supportingInformationFormat = supportingInformationFormat,
    deliveryAddressType = deliveryAddressType,
    collectionReason = collectionReason,
    elector = elector,
    status = status,
    photoUrl = photoUrl,
    issueDate = issueDate,
    userId = userId,
    dateTime = dateTime,
)

fun buildAnonymousElectorApi(
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    addressee: String = aValidDeliveryName(),
    registeredAddress: Address = buildValidAddress(),
    email: String? = aValidEmailAddress(),
    phoneNumber: String? = aValidPhoneNumber()
): AnonymousElector = AnonymousElector(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    addressee = addressee,
    registeredAddress = registeredAddress,
    email = email,
    phoneNumber = phoneNumber
)
