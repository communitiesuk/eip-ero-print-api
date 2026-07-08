package uk.gov.dluhc.printapi.testsupport.emails

import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.codec.json.JacksonJsonDecoder
import org.springframework.http.codec.json.JacksonJsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import tools.jackson.databind.json.JsonMapper
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * This class allows access to Localstack's list of sent emails.
 */
@Component
class LocalstackEmailMessagesSentClient(
    private val httpClient: ClientHttpConnector,
    private val jsonMapper: JsonMapper,
    private val localStackContainerSettings: LocalStackContainerConfiguration.LocalStackContainerSettings,
) {

    fun getEmailMessagesSent(): LocalstackEmailMessages {
        val webClient = WebClient.builder()
            .clientConnector(httpClient)
            .codecs { configurer ->
                configurer.defaultCodecs().jacksonJsonEncoder(JacksonJsonEncoder(jsonMapper, MediaType.APPLICATION_JSON))
                configurer.defaultCodecs().jacksonJsonDecoder(JacksonJsonDecoder(jsonMapper, MediaType.APPLICATION_JSON))
            }
            .build()

        val emailServerUri = URI.create(localStackContainerSettings.sesMessagesUrl)
        val response = webClient.get().uri(emailServerUri)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .bodyToMono(LocalstackEmailMessages::class.java)
            .onErrorResume { ex -> handleException(ex, "Error getting email messages from Localstack") }
            .block()

        return response!!
    }

    private fun handleException(ex: Throwable, message: String): Mono<out LocalstackEmailMessages> {
        logger.error(ex) { "Unhandled exception thrown by WebClient" }
        return Mono.error(RuntimeException(message))
    }
}
