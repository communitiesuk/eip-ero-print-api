package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssuingAuthority
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidTemporaryCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidTemporaryCertificateTemplateFilename
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun buildTemporaryCertificate(
    id: UUID? = UUID.randomUUID(),
    certificateNumber: String = aValidVacNumber(),
    gssCode: String = aGssCode(),
    sourceType: SourceType = aValidSourceType(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateTemplateFilename: String = aValidTemporaryCertificateTemplateFilename(),
    issuingAuthority: String = aValidIssuingAuthority(),
    issuingAuthorityCy: String? = null,
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    certificateLanguage: CertificateLanguage = aValidCertificateLanguage(),
    photoLocationArn: String = aPhotoArn(),
    issueDate: LocalDate = aValidIssueDate(),
    validOnDate: LocalDate = aValidIssueDate(),
    statusHistory: List<TemporaryCertificateStatus> = listOf(
        buildTemporaryCertificateStatus()
    ),
    userId: String = aValidUserId(),
): TemporaryCertificate {
    return TemporaryCertificate(
        id = id,
        certificateNumber = certificateNumber,
        gssCode = gssCode,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        certificateTemplateFilename = certificateTemplateFilename,
        issuingAuthority = issuingAuthority,
        issuingAuthorityCy = issuingAuthorityCy,
        firstName = firstName,
        middleNames = middleNames,
        surname = surname,
        certificateLanguage = certificateLanguage,
        photoLocationArn = photoLocationArn,
        issueDate = issueDate,
        validOnDate = validOnDate,
        userId = userId
    ).apply {
        this.statusHistory.addAll(statusHistory)
    }
}

fun buildTemporaryCertificateStatus(
    status: TemporaryCertificateStatus.Status = aValidTemporaryCertificateStatus(),
    userId: String = aValidUserId(),
    dateCreated: Instant? = null
): TemporaryCertificateStatus {
    return TemporaryCertificateStatus(
        status = status,
        userId = userId,
        dateCreated = dateCreated,
    )
}
