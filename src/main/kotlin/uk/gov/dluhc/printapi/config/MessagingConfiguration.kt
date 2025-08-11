package uk.gov.dluhc.printapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.operations.SqsTemplate
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.sqs.SqsAsyncClient
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.messagingsupport.MessagingConfigurationHelper
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateApplicationStatisticsMessage

@Configuration
class MessagingConfiguration {

    @Value("\${sqs.maximum-number-of-concurrent-messages}")
    private lateinit var maximumNumberOfConcurrentMessages: Number

    @Value("\${sqs.process-print-request-batch-queue-name}")
    private lateinit var processPrintRequestBatchQueueName: String

    @Value("\${sqs.process-print-response-file-queue-name}")
    private lateinit var processPrintResponseFileQueueName: String

    @Value("\${sqs.process-print-response-queue-name}")
    private lateinit var processPrintResponseQueueName: String

    @Value("\${sqs.remove-certificate-queue-name}")
    private lateinit var removeCertificateQueueName: String

    @Value("\${sqs.trigger-voter-card-statistics-update-queue-name}")
    private lateinit var triggerVoterCardStatisticsUpdateQueueName: String

    @Value("\${sqs.trigger-application-statistics-update-queue-name}")
    private lateinit var triggerApplicationStatisticsUpdateQueueName: String

    /**
     * We also construct an sqsTemplate when running integration tests, and this
     * definition conflicts with it, so hide this Bean if running under test.
     */
    @Bean
    @Primary
    @Profile("!integration-test")
    fun sqsTemplate(
        sqsAsyncClient: SqsAsyncClient,
        sqsMessagingMessageConverter: SqsMessagingMessageConverter
    ) = MessagingConfigurationHelper.sqsTemplate(sqsAsyncClient, sqsMessagingMessageConverter)

    @Bean
    fun processPrintRequestBatchQueue(sqsTemplate: SqsTemplate) =
        MessageQueue<ProcessPrintRequestBatchMessage>(processPrintRequestBatchQueueName, sqsTemplate)

    @Bean
    fun processPrintResponseFileQueue(sqsTemplate: SqsTemplate) =
        MessageQueue<ProcessPrintResponseFileMessage>(processPrintResponseFileQueueName, sqsTemplate)

    @Bean
    fun processPrintResponseQueue(sqsTemplate: SqsTemplate) =
        MessageQueue<ProcessPrintResponseMessage>(processPrintResponseQueueName, sqsTemplate)

    @Bean
    fun removeCertificateQueue(sqsTemplate: SqsTemplate) =
        MessageQueue<RemoveCertificateMessage>(removeCertificateQueueName, sqsTemplate)

    @Bean
    fun triggerApplicationStatisticsUpdateQueue(sqsTemplate: SqsTemplate) =
        MessageQueue<UpdateApplicationStatisticsMessage>(triggerApplicationStatisticsUpdateQueueName, sqsTemplate)

    @Bean
    fun sqsMessagingMessageConverter(
        objectMapper: ObjectMapper
    ) = MessagingConfigurationHelper.sqsMessagingMessageConverter(objectMapper)

    @Bean
    fun defaultSqsListenerContainerFactory(
        sqsAsyncClient: SqsAsyncClient,
        sqsMessagingMessageConverter: SqsMessagingMessageConverter,
    ) = MessagingConfigurationHelper.defaultSqsListenerContainerFactory(
        sqsAsyncClient,
        sqsMessagingMessageConverter,
        maximumNumberOfConcurrentMessages.toInt(),
    )
}
