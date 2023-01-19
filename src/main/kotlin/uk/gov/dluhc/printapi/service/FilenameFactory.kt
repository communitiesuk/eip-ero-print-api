package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class FilenameFactory(private val clock: Clock) {

    fun createZipFilename(batchId: String, certificates: List<Certificate>): String {
        return createFilename(batchId, printRequestCount(batchId, certificates), "zip")
    }

    fun createPrintRequestsFilename(batchId: String, certificates: List<Certificate>): String {
        return createFilename(batchId, printRequestCount(batchId, certificates), "psv")
    }

    private fun printRequestCount(batchId: String, certificates: List<Certificate>): Int {
        return certificates
            .flatMap { it.printRequests }
            .count { it.getCurrentStatus().status == Status.ASSIGNED_TO_BATCH && it.batchId == batchId }
    }

    private fun createFilename(batchId: String, count: Int, ext: String): String {
        val timestamp = LocalDateTime.now(clock).toInstant(ZoneOffset.UTC)
        val formattedTimestamp = TIMESTAMP_FORMATTER.withZone(ZoneId.of("UTC")).format(timestamp)
        return "$batchId-$formattedTimestamp-$count.$ext"
    }

    companion object {
        private var TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    }
}
