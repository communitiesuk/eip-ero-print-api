package uk.gov.dluhc.printapi.dto

import java.time.Instant

data class VacPrintRequestSummaryDto(
    val status: PrintRequestStatusDto,
    val dateTime: Instant,
    val userId: String,
)
