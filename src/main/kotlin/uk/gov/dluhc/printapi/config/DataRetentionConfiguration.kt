package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Period

@ConfigurationProperties(prefix = "api.print-api.data-retention")
@ConstructorBinding
data class DataRetentionConfiguration(
    val certificateInitialRetentionPeriod: Period,
    val certificateRemovalBatchSize: Int
)
