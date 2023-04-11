package uk.gov.dluhc.logging.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.dluhc.printapi.config.IntegrationTest
import java.util.UUID

internal class CorrelationIdWebClientMdcExchangeFilterTest : IntegrationTest() {

    @Autowired
    private lateinit var correlationIdExchangeFilter: CorrelationIdWebClientMdcExchangeFilter

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    private lateinit var webClient: WebTestClient

    @BeforeEach
    fun setup() {
        stubExternalRestApi()
        MDC.clear()
        webClient = WebTestClient.bindToServer()
            .baseUrl(wireMockService.wiremockBaseUrl())
            .filter(correlationIdExchangeFilter)
            .build()
    }

    @Test
    fun `should add correlation-id to http request given WebClient request with no correlationId in MDC`() {
        // Given

        // When
        webClient.get()
            .uri("/external-rest-api")
            .exchange()

        // Then
        verifyExternalRestApiCalledWithCorrelationId(WireMock.matching("^[a-z0-9]+$"))
    }

    @Test
    fun `should add correlation-id to http request given WebClient request with correlationId set in MDC`() {
        // Given
        val expectedCorrelationId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("correlationId", expectedCorrelationId)

        // When
        webClient.get()
            .uri("/external-rest-api")
            .exchange()

        // Then
        verifyExternalRestApiCalledWithCorrelationId(WireMock.equalTo(expectedCorrelationId))
    }

    fun stubExternalRestApi() {
        wireMockServer.stubFor(
            WireMock.get(WireMock.urlPathMatching("/external-rest-api"))
                .willReturn(
                    ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                )
        )
    }

    fun verifyExternalRestApiCalledWithCorrelationId(correlationIdMatcher: StringValuePattern) {
        wireMockServer.verify(
            1,
            WireMock.getRequestedFor(WireMock.urlEqualTo("/external-rest-api"))
                .withHeader("x-correlation-id", correlationIdMatcher)
        )
    }
}
