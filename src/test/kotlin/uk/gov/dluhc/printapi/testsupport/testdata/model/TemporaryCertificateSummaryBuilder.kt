package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.models.TemporaryCertificateSummary
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidOnDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import java.time.LocalDate
import java.time.OffsetDateTime

fun buildTemporaryCertificateSummary(
    certificateNumber: String = aValidCertificateNumber(),
    status: TemporaryCertificateStatus = TemporaryCertificateStatus.GENERATED,
    userId: String = aValidUserId(),
    dateTimeGenerated: OffsetDateTime = aValidGeneratedDateTime(),
    issueDate: LocalDate = aValidIssueDate(),
    validOnDate: LocalDate = aValidOnDate(),
): TemporaryCertificateSummary =
    TemporaryCertificateSummary(
        certificateNumber = certificateNumber,
        status = status,
        userId = userId,
        dateTimeGenerated = dateTimeGenerated,
        issueDate = issueDate,
        validOnDate = validOnDate,
    )
