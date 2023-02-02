package uk.gov.dluhc.printapi.dto

import java.time.Instant

data class PrintRequestSummaryDto(
    val status: PrintRequestStatusDto,
    val dateTime: Instant,
    val userId: String,
    val message: String?
)
