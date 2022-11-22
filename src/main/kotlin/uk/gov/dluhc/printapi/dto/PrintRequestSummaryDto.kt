package uk.gov.dluhc.printapi.dto

import java.time.Instant

data class PrintRequestSummaryDto(
    val status: StatusDto,
    val dateTime: Instant,
    val userId: String,
    val message: String?
)
