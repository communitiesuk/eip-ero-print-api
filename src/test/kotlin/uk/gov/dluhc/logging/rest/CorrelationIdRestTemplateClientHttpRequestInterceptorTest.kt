package uk.gov.dluhc.logging.rest

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import uk.gov.dluhc.printapi.config.IntegrationTest
import java.util.UUID

internal class CorrelationIdRestTemplateClientHttpRequestInterceptorTest : IntegrationTest() {

    @Autowired
    private lateinit var correlationIdRestTemplateClientHttpRequestInterceptor: CorrelationIdRestTemplateClientHttpRequestInterceptor

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setup() {
        stubExternalRestApi()
        MDC.clear()
        restTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .rootUri(wireMockService.wiremockBaseUrl())
                .interceptors(correlationIdRestTemplateClientHttpRequestInterceptor)
        )
    }

    @Test
    fun `should add correlation-id to http request given WebClient request with no correlationId in MDC`() {
        // Given

        // When
        restTemplate.getForObject("/external-rest-api", Void::class.java)

        // Then
        verifyExternalRestApiCalledWithCorrelationId(WireMock.matching("^[a-z0-9]+$"))
    }

    @Test
    fun `should add correlation-id to http request given WebClient request with correlationId set in MDC`() {
        // Given
        val expectedCorrelationId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("correlationId", expectedCorrelationId)

        // When
        restTemplate.getForObject("/external-rest-api", Void::class.java)

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
