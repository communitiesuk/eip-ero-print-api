package uk.gov.dluhc.printapi.config

import io.awspring.cloud.messaging.config.QueueMessageHandlerFactory
import io.awspring.cloud.messaging.listener.support.AcknowledgmentHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.listener.support.VisibilityHandlerMethodArgumentResolver
import io.awspring.cloud.messaging.support.NotificationSubjectArgumentResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.handler.annotation.support.HeadersMethodArgumentResolver
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver
import org.springframework.validation.Validator

@Configuration
class MessagingConfiguration {
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
