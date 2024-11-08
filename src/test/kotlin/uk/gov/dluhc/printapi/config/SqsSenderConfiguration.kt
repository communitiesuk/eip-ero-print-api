package uk.gov.dluhc.printapi.config

import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.dluhc.messagingsupport.MessagingConfigurationHelper

@Configuration
class SqsSenderConfiguration {

    /*
     * For integration tests to function against the correct LocalStack instance,
     * it's necessary for the correct value of the property cloud.aws.sqs.endpoint to be set prior to the amazonSQS bean being created.
     * This property is set programmatically because it is determined after the LocalStack/queues have been created.
     * This happens in uk.gov.dluhc.votercardapplicationsapi.config.LocalStackContainerConfiguration.localStackContainerSettings()
     *
     * The amazonSQS spring bean is LAZILY created in  io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration.SqsClientConfiguration.amazonSQS()
     *
     * This ordering is achieved implicitly by the @DependsOn annotation below.
     */
    @Bean
    @DependsOn("localStackContainerSettings")
    fun sqsTemplate(
        sqsAsyncClient: SqsAsyncClient,
        sqsMessagingMessageConverter: SqsMessagingMessageConverter
    ) = MessagingConfigurationHelper.sqsTemplate(sqsAsyncClient, sqsMessagingMessageConverter)
}
