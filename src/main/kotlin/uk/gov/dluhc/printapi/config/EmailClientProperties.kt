package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

/**
 * Class to bind configuration properties for the email client
 */
@ConfigurationProperties(prefix = "email.client")
@ConstructorBinding
data class EmailClientProperties(
    val sender: String,
    val allowListEnabled: Boolean,
    val allowListDomains: Set<String>,
)
