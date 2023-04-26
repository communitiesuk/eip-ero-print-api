package uk.gov.dluhc.printapi.dto.aed

import java.time.Instant
import java.time.LocalDate

data class AnonymousSearchSummaryPageDto(
    val results: List<AnonymousSearchSummaryDto>,
)

data class AnonymousSearchSummaryDto(
    val gssCode: String,
    val sourceReference: String,
    val applicationReference: String,
    val certificateNumber: String,
    val electoralRollNumber: String,
    val firstName: String,
    val surname: String,
    val postcode: String,
    val issueDate: LocalDate,
    val dateTimeCreated: Instant,
)
