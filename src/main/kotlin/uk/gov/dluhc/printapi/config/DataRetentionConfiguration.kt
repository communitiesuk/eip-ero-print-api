package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Period

@ConfigurationProperties(prefix = "api.print-api.data-retention")
data class DataRetentionConfiguration(
    val certificateInitialRetentionPeriod: Period,
    val certificateRemovalBatchSize: Int,
)
