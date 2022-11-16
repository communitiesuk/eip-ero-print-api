package uk.gov.dluhc.printapi.exception

import uk.gov.dluhc.printapi.database.entity.Status
import java.lang.RuntimeException

data class EmptyBatchException(
    private val batchId: String,
    private val status: Status
) : RuntimeException("No certificates found for batchId = $batchId and status = $status")
