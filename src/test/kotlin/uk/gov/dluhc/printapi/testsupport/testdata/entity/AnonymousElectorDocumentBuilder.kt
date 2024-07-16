package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestStatusEventDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.anAnonymousElectorDocumentSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun buildAnonymousElectorDocument(
    persisted: Boolean = false,
    gssCode: String = aGssCode(),
    sourceType: SourceType = anAnonymousElectorDocumentSourceType(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateNumber: String = aValidVacNumber(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    photoLocationArn: String = aPhotoArn(),
    surname: String = aValidSurname(),
    contactDetails: AedContactDetails = buildAedContactDetails(surname = surname),
    aedTemplateFilename: String = aValidAnonymousElectorDocumentTemplateFilename(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    issueDate: LocalDate = aValidIssueDate(),
    aedStatuses: List<AnonymousElectorDocumentStatus> = listOf(buildAnonymousElectorDocumentStatus()),
    requestDateTime: Instant = aValidRequestDateTime(),
    userId: String = aValidUserId(),
    delivery: Delivery = buildDelivery(),
    initialRetentionRemovalDate: LocalDate? = null,
    initialRetentionDataRemoved: Boolean = false,
    finalRetentionRemovalDate: LocalDate? = null,
): AnonymousElectorDocument {
    return AnonymousElectorDocument(
        id = if (persisted) UUID.randomUUID() else null,
        gssCode = gssCode,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        certificateNumber = certificateNumber,
        certificateLanguage = certificateLanguage,
        supportingInformationFormat = supportingInformationFormat,
        photoLocationArn = photoLocationArn,
        contactDetails = contactDetails,
        initialRetentionRemovalDate = initialRetentionRemovalDate,
        initialRetentionDataRemoved = initialRetentionDataRemoved,
        finalRetentionRemovalDate = finalRetentionRemovalDate,
        aedTemplateFilename = aedTemplateFilename,
        electoralRollNumber = electoralRollNumber,
        issueDate = issueDate,
        requestDateTime = requestDateTime,
        userId = userId,
        delivery = delivery,
        dateCreated = if (persisted) Instant.now() else null,
        createdBy = if (persisted) "system" else null,
    ).also {
        aedStatuses.forEach { electorDocumentStatus -> it.addStatus(electorDocumentStatus) }
    }
}

fun buildAnonymousElectorDocumentStatus(
    persisted: Boolean = false,
    status: AnonymousElectorDocumentStatus.Status = aValidAnonymousElectorDocumentStatus(),
    eventDateTime: Instant = aValidPrintRequestStatusEventDateTime(),
): AnonymousElectorDocumentStatus {
    return AnonymousElectorDocumentStatus(
        id = if (persisted) UUID.randomUUID() else null,
        status = status,
        eventDateTime = eventDateTime,
        dateCreated = if (persisted) Instant.now() else null,
        createdBy = if (persisted) "system" else null,
    )
}

fun buildAedContactDetails(
    persisted: Boolean = false,
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    address: Address = buildAddress(),
    email: String = aValidEmailAddress(),
    phoneNumber: String = aValidPhoneNumber(),
): AedContactDetails = AedContactDetails(
    id = if (persisted) UUID.randomUUID() else null,
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    address = address,
    email = email,
    phoneNumber = phoneNumber,
    dateCreated = if (persisted) Instant.now() else null,
    createdBy = if (persisted) "system" else null,
    dateUpdated = if (persisted) Instant.now() else null,
    updatedBy = if (persisted) "system" else null,
)
