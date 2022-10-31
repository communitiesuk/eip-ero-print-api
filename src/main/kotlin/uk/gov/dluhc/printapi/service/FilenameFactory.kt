package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class FilenameFactory(private val clock: Clock) {

    fun createZipFilename(batchId: String, count: Int): String = createFilename(batchId, count, "zip")

    fun createPrintRequestsFilename(batchId: String, count: Int): String = createFilename(batchId, count, "psv")

    private fun createFilename(batchId: String, count: Int, ext: String): String {
        val timestamp = LocalDateTime.now(clock).toInstant(ZoneOffset.UTC)
        return "$batchId-${TIMESTAMP_FORMATTER.withZone(ZoneId.of("UTC")).format(timestamp)}-$count.$ext"
    }

    companion object {
        private var TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    }
}
