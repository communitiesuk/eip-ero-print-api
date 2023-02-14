package uk.gov.dluhc.printapi.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class TemporaryCertificateSummaryDto(
    val certificateNumber: String,
    val status: TemporaryCertificateStatusDto,
    val userId: String,
    val dateTimeGenerated: OffsetDateTime,
    val issueDate: LocalDate,
    val validOnDate: LocalDate,
)
