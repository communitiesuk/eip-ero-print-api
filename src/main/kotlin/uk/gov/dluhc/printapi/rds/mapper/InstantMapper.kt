package uk.gov.dluhc.printapi.rds.mapper

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class InstantMapper {
    fun toInstant(offsetDateTime: OffsetDateTime?): Instant? = offsetDateTime?.toInstant()

    fun toOffsetDateTime(instant: Instant?): OffsetDateTime? = instant?.atOffset(ZoneOffset.UTC)
}
