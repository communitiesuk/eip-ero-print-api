package uk.gov.dluhc.printapi.config

import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.listener.support.VisibilityHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.support.NotificationSubjectArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver
import org.springframework.validation.Validator
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintBatchStatusUpdateMessage
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage

@Configuration
class MessagingConfiguration {

    @Value("\${sqs.process-print-request-batch-queue-name}")
    private lateinit var processPrintRequestBatchQueueName: String

    @Value("\${sqs.process-print-response-file-queue-name}")
    private lateinit var processPrintResponseFileQueueName: String

    @Bean
    fun processPrintRequestBatchQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<ProcessPrintRequestBatchMessage>(processPrintRequestBatchQueueName, queueMessagingTemplate)

    @Bean
    fun processPrintResponseFileQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<ProcessPrintBatchStatusUpdateMessage>(processPrintResponseFileQueueName, queueMessagingTemplate)

    @Bean
    fun queueMessageHandlerFactory(
        jacksonMessageConverter: MappingJackson2MessageConverter,
        hibernateValidator: Validator
    ): QueueMessageHandlerFactory =
        QueueMessageHandlerFactory().apply {
            setArgumentResolvers(
                listOf(
                    HeadersMethodArgumentResolver(),
                    NotificationSubjectArgumentResolver(),
                    AcknowledgmentHandlerMethodArgumentResolver("Acknowledgment"),
                    VisibilityHandlerMethodArgumentResolver("Visibility"),
                    PayloadMethodArgumentResolver(jacksonMessageConverter, hibernateValidator)
                )
            )
        }
}
