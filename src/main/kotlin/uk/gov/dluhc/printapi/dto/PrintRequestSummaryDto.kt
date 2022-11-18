package uk.gov.dluhc.printapi.dto

import uk.gov.dluhc.printapi.database.entity.Status
import java.time.Instant

data class PrintRequestSummaryDto(
    private val status: Status,
    private val dateTime: Instant,
    private val userId: String,
    private val message: String?
)
