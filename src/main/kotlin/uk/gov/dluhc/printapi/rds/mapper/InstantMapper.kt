package uk.gov.dluhc.printapi.rds.mapper

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime

@Component
class InstantMapper {
    fun toInstant(offsetDateTime: OffsetDateTime?): Instant? = offsetDateTime?.toInstant()
}
