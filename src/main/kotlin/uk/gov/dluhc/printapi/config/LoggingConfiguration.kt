package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.dluhc.logging.config.CorrelationIdMdcInterceptor
import uk.gov.dluhc.logging.config.CorrelationIdMdcMessageListenerAspect
import uk.gov.dluhc.logging.config.CorrelationIdMdcScheduledAspect

@Configuration
class LoggingConfiguration {

    @Bean
    fun correlationIdMdcInterceptor() = CorrelationIdMdcInterceptor()

    @Bean
    fun correlationIdMdcMessageListenerAspect() = CorrelationIdMdcMessageListenerAspect()

    @Bean
    fun correlationIdMdcScheduledAspect() = CorrelationIdMdcScheduledAspect()
}
