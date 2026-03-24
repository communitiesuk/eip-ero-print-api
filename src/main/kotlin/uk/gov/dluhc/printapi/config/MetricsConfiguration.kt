package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "metrics")
data class MetricsConfiguration(
    val environment: String,
    val metricsEnabled: Boolean,
)
