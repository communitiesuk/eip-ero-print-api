package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class FilenameFactory(private val clock: Clock) {

    fun createZipFilename(batchId: String, certificates: List<Certificate>): String {
        return createFilename(batchId, countPrintRequestsAssignedToBatch(certificates, batchId), "zip")
    }

    fun createPrintRequestsFilename(batchId: String, certificates: List<Certificate>): String {
        return createFilename(batchId, countPrintRequestsAssignedToBatch(certificates, batchId), "psv")
    }

    private fun createFilename(batchId: String, count: Int, ext: String): String {
        val timestamp = LocalDateTime.now(clock).toInstant(ZoneOffset.UTC)
        val formattedTimestamp = TIMESTAMP_FORMATTER.withZone(ZoneId.of("UTC")).format(timestamp)
        return "$batchId-$formattedTimestamp-$count.$ext"
    }

    companion object {
        private val TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    }
}
