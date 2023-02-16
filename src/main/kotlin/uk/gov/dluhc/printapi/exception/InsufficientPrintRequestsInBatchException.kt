package uk.gov.dluhc.printapi.exception

import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import java.lang.RuntimeException

data class InsufficientPrintRequestsInBatchException(
    private val batchId: String,
    private val status: Status,
    private val foundCount: Int,
    private val expectedCount: Int?
) : RuntimeException("Found $foundCount ${if (expectedCount != null) "of $expectedCount " else ""}certificates for batchId = $batchId and status = $status")
