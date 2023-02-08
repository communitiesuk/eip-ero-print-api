package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConfigurationProperties(prefix = "api.print-api.retention.period")
@ConstructorBinding
data class DataRetentionConfiguration(
    val certificateDeliveryInfo: Duration
)
