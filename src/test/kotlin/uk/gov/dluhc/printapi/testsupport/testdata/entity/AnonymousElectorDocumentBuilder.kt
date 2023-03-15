package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAnonymousElectorDocumentTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
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
    id: UUID? = UUID.randomUUID(),
    gssCode: String = aGssCode(),
    sourceType: SourceType = anAnonymousElectorDocumentSourceType(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateNumber: String = aValidVacNumber(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    photoLocationArn: String = aPhotoArn(),
    contactDetails: AedContactDetails = buildAedContactDetails(),
    aedTemplateFilename: String = aValidAnonymousElectorDocumentTemplateFilename(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    issueDate: LocalDate = aValidIssueDate(),
    aedStatuses: List<AnonymousElectorDocumentStatus> = listOf(buildAnonymousElectorDocumentStatus()),
    requestDateTime: Instant = aValidRequestDateTime(),
    userId: String = aValidUserId(),
    initialRetentionRemovalDate: LocalDate? = null,
    initialRetentionDataRemoved: Boolean = false,
    finalRetentionRemovalDate: LocalDate? = null,
): AnonymousElectorDocument {
    return AnonymousElectorDocument(
        id = id,
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
    ).also {
        aedStatuses.forEach { electorDocumentStatus -> it.addStatus(electorDocumentStatus) }
    }
}

fun buildAnonymousElectorDocumentStatus(
    status: AnonymousElectorDocumentStatus.Status = aValidAnonymousElectorDocumentStatus(),
    eventDateTime: Instant = aValidPrintRequestStatusEventDateTime(),
): AnonymousElectorDocumentStatus {
    return AnonymousElectorDocumentStatus(
        status = status,
        eventDateTime = eventDateTime,
    )
}

fun buildAedContactDetails(
    firstName: String = aValidFirstName(),
    surname: String = aValidSurname(),
    address: Address = buildAddress(),
): AedContactDetails = AedContactDetails(
    firstName = firstName,
    surname = surname,
    address = address,
)
