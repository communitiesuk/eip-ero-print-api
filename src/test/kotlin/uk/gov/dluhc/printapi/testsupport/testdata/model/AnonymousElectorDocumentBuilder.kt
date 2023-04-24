package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.Address
import uk.gov.dluhc.printapi.models.AnonymousElector
import uk.gov.dluhc.printapi.models.AnonymousElectorDocument
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat
import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.LocalDate
import java.time.OffsetDateTime

fun buildAnonymousElectorDocument(
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    gssCode: String = aGssCode(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: AnonymousSupportingInformationFormat = AnonymousSupportingInformationFormat.STANDARD,
    deliveryAddressType: DeliveryAddressType = DeliveryAddressType.REGISTERED,
    elector: AnonymousElector = buildAnonymousElector(),
    status: AnonymousElectorDocumentStatus = AnonymousElectorDocumentStatus.PRINTED,
    photoLocation: String = aPhotoArn(),
    issueDate: LocalDate = aValidIssueDate(),
    userId: String = aValidUserId(),
    dateTime: OffsetDateTime = aValidGeneratedDateTime()
): AnonymousElectorDocument = AnonymousElectorDocument(
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
