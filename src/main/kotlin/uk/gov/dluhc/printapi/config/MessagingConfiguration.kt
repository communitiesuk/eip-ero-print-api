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
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateApplicationStatisticsMessage
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateStatisticsMessage

@Configuration
class MessagingConfiguration {

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

    @Bean
    fun processPrintRequestBatchQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<ProcessPrintRequestBatchMessage>(processPrintRequestBatchQueueName, queueMessagingTemplate)

    @Bean
    fun processPrintResponseFileQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<ProcessPrintResponseFileMessage>(processPrintResponseFileQueueName, queueMessagingTemplate)

    @Bean
    fun processPrintResponseQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<ProcessPrintResponseMessage>(processPrintResponseQueueName, queueMessagingTemplate)

    @Bean
    fun removeCertificateQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<RemoveCertificateMessage>(removeCertificateQueueName, queueMessagingTemplate)

    @Bean
    fun triggerVoterCardStatisticsUpdateQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<UpdateStatisticsMessage>(triggerVoterCardStatisticsUpdateQueueName, queueMessagingTemplate)

    @Bean
    fun triggerApplicationStatisticsUpdateQueue(queueMessagingTemplate: QueueMessagingTemplate) =
        MessageQueue<UpdateApplicationStatisticsMessage>(triggerApplicationStatisticsUpdateQueueName, queueMessagingTemplate)

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
