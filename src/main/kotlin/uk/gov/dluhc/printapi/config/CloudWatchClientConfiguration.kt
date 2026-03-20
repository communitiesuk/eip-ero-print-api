package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient

@Configuration
class CloudWatchClientConfiguration {
    @Bean
    fun cloudWatchClient(): CloudWatchClient =
        CloudWatchClient.builder()
            .region(DefaultAwsRegionProviderChain().region)
            .build()
}
