package uk.gov.dluhc.printapi.testsupport.testdata.dto.aed

import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.CertificateLanguage
import uk.gov.dluhc.printapi.dto.DeliveryAddressType
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguageDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.LocalDate

fun buildAnonymousElectorDocumentDto(
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    gssCode: String = aGssCode(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateLanguage: CertificateLanguage = aValidCertificateLanguageDto(),
    supportingInformationFormat: AnonymousSupportingInformationFormat? = AnonymousSupportingInformationFormat.STANDARD,
    deliveryAddressType: DeliveryAddressType? = DeliveryAddressType.REGISTERED,
    collectionReason: String? = null,
    elector: AnonymousElectorDto = buildAnonymousElectorDto(),
    status: AnonymousElectorDocumentStatus = AnonymousElectorDocumentStatus.PRINTED,
    photoLocationArn: String = aPhotoArn(),
    issueDate: LocalDate = aValidIssueDate(),
    userId: String = aValidUserId(),
    requestDateTime: Instant = aValidApplicationReceivedDateTime()
): AnonymousElectorDocumentDto = AnonymousElectorDocumentDto(
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
    photoLocationArn = photoLocationArn,
    issueDate = issueDate,
    userId = userId,
    requestDateTime = requestDateTime
)

fun buildAnonymousElectorDto(
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    addressee: String = aValidDeliveryName(),
    registeredAddress: AddressDto? = buildValidAddressDto(),
    email: String? = aValidEmailAddress(),
    phoneNumber: String? = aValidPhoneNumber()
): AnonymousElectorDto = AnonymousElectorDto(
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    addressee = addressee,
    registeredAddress = registeredAddress,
    email = email,
    phoneNumber = phoneNumber
)
