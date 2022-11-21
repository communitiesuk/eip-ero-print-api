package uk.gov.dluhc.printapi.dto

import java.time.Instant

data class PrintRequestSummaryDto(
    private val status: StatusDto,
    private val dateTime: Instant,
    private val userId: String,
    private val message: String?
)
