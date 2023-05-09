package uk.gov.dluhc.printapi.mapper.aed

import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Component
class AedMappingHelper(private val clock: Clock) {

    fun issueDate(): LocalDate = LocalDate.now(clock)

    fun requestDateTime(): Instant = Instant.now(clock)

    fun statusHistory(status: AnonymousElectorDocumentStatus.Status): List<AnonymousElectorDocumentStatus> {
        return listOf(
            AnonymousElectorDocumentStatus(
                status = status,
                eventDateTime = Instant.now(clock)
            )
        )
    }
}
