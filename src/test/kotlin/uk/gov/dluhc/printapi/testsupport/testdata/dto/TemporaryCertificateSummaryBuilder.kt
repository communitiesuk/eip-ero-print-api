package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.TemporaryCertificateStatusDto
import uk.gov.dluhc.printapi.dto.TemporaryCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidOnDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import java.time.LocalDate
import java.time.OffsetDateTime

fun buildTemporaryCertificateSummaryDto(
    certificateNumber: String = aValidCertificateNumber(),
    status: TemporaryCertificateStatusDto = TemporaryCertificateStatusDto.GENERATED,
    userId: String = aValidUserId(),
    dateTimeGenerated: OffsetDateTime = aValidGeneratedDateTime(),
    issueDate: LocalDate = aValidIssueDate(),
    validOnDate: LocalDate = aValidOnDate(),
): TemporaryCertificateSummaryDto =
    TemporaryCertificateSummaryDto(
        certificateNumber = certificateNumber,
        status = status,
        userId = userId,
        dateTimeGenerated = dateTimeGenerated,
        issueDate = issueDate,
        validOnDate = validOnDate,
    )
