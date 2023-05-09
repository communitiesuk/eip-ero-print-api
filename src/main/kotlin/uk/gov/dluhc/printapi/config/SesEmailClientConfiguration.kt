package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain
import software.amazon.awssdk.services.ses.SesClient
import uk.gov.dluhc.emailnotifications.SesEmailClient

/**
 * Configuration class exposing a configured email client bean to send emails via AWS's SES service.
 */
@Configuration
class SesEmailClientConfiguration() {
    @Bean
    fun sesClient(): SesClient =
        SesClient.builder()
            .region(DefaultAwsRegionProviderChain().region)
            .build()

    @Bean
    fun sesEmailClient(
        sesClient: SesClient,
        emailClientProperties: EmailClientProperties,
    ): SesEmailClient =
        with(emailClientProperties) {
            SesEmailClient(
                sesClient = sesClient,
                sender = sender,
                allowListEnabled = allowListEnabled,
                allowListDomains = allowListDomains,
            )
        }
}
