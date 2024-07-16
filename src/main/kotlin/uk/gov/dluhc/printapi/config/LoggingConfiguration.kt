package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.dluhc.logging.config.CorrelationIdMdcScheduledAspect
import uk.gov.dluhc.logging.rest.CorrelationIdMdcInterceptor
import uk.gov.dluhc.logging.rest.CorrelationIdRestTemplateClientHttpRequestInterceptor
import uk.gov.dluhc.logging.rest.CorrelationIdWebClientMdcExchangeFilter

@Configuration
class LoggingConfiguration {

    @Bean
    fun correlationIdMdcInterceptor() = CorrelationIdMdcInterceptor()

    @Bean
    fun correlationIdMdcScheduledAspect() = CorrelationIdMdcScheduledAspect()

    @Bean
    fun correlationIdRestTemplateClientHttpRequestInterceptor() =
        CorrelationIdRestTemplateClientHttpRequestInterceptor()

    @Bean
    fun correlationIdWebClientMdcExchangeFilter() = CorrelationIdWebClientMdcExchangeFilter()
}
