package uk.gov.dluhc.printapi.config

import ch.qos.logback.classic.Level
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.logging.config.CORRELATION_ID
import uk.gov.dluhc.logging.testsupport.assertj.assertions.ILoggingEventAssert.Companion.assertThat
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.TestLogAppender.Companion.logs
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAdminBearerToken
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit

/**
 * Integration tests that assert the correlation ID is correctly applied to log statements via the Interceptors and Aspects
 * in LoggingConfiguration
 *
 * The tests in this class assert the cross-cutting logging behaviour. They do not assert the behaviour or output of any
 * bean or code that is used to tests.
 */
internal class LoggingConfigurationIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var correlationIdExchangeFilter: CorrelationIdWebClientMdcExchangeFilter

    @Autowired
    private lateinit var correlationIdRestTemplateClientHttpRequestInterceptor: CorrelationIdRestTemplateClientHttpRequestInterceptor

    @Autowired
    private lateinit var testScheduledJob: TestScheduledJob

    @Autowired
    private lateinit var queueMessagingTemplate: QueueMessagingTemplate

    @Autowired
    private lateinit var wireMockServer: WireMockServer

    @Nested
    inner class RestControllerCorrelationIdMdcInterceptor {
        private val URI_TEMPLATE = "/correlation-id-test-api"

        @BeforeEach
        fun setup() {
            wireMockService.stubCognitoJwtIssuerResponse()
            TestLogAppender.reset()
            MDC.clear()
        }

        @Test
        fun `should add correlation-id to request and MDC given REST API is called with no correlation-id header`() {
            // Given

            // When
            webTestClient.get()
                .uri(URI_TEMPLATE)
                .bearerToken(getVCAdminBearerToken())
                .exchange()
                .expectHeader().exists("x-correlation-id")

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Test API successfully called",
                        Level.INFO
                    )
                ).hasAnyCorrelationId()
            }
        }

        @Test
        fun `should service REST API call given correlation-id header`() {
            // Given
            val expectedCorrelationId = randomUUID().toString().replace("-", "")

            // When
            webTestClient.get()
                .uri(URI_TEMPLATE)
                .bearerToken(getVCAdminBearerToken())
                .header("x-correlation-id", expectedCorrelationId)
                .exchange()
                .expectHeader().valueEquals("x-correlation-id", expectedCorrelationId)

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Test API successfully called",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
            }
        }
    }

    @Nested
    inner class WebClientCorrelationIdExchangeFilter {

        private val webClient = WebTestClient
            .bindToServer()
            .baseUrl(wireMockService.wiremockBaseUrl())
            .filter(correlationIdExchangeFilter)
            .build()

        @BeforeEach
        fun setup() {
            stubExternalRestApi()
            MDC.clear()
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
            val expectedCorrelationId = randomUUID().toString().replace("-", "")
            MDC.put("correlationId", expectedCorrelationId)

            // When
            webClient.get()
                .uri("/external-rest-api")
                .exchange()

            // Then
            verifyExternalRestApiCalledWithCorrelationId(equalTo(expectedCorrelationId))
        }
    }

    @Nested
    inner class RestTemplateCorrelationIdInterceptor {

        private val restTemplate = TestRestTemplate(
            RestTemplateBuilder()
                .rootUri(wireMockService.wiremockBaseUrl())
                .interceptors(correlationIdRestTemplateClientHttpRequestInterceptor)
        )

        @BeforeEach
        fun setup() {
            stubExternalRestApi()
            MDC.clear()
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
            val expectedCorrelationId = randomUUID().toString().replace("-", "")
            MDC.put("correlationId", expectedCorrelationId)

            // When
            restTemplate.getForObject("/external-rest-api", Void::class.java)

            // Then
            verifyExternalRestApiCalledWithCorrelationId(equalTo(expectedCorrelationId))
        }
    }

    @Nested
    inner class CorrelationIdMdcScheduledJobAspect {

        @BeforeEach
        fun setup() {
            TestLogAppender.reset()
            MDC.clear()
        }

        @Test
        fun `should add correlationId to MDC`() {
            // Given

            // When
            testScheduledJob.run()

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Test scheduled job successfully called",
                        Level.INFO
                    )
                ).hasAnyCorrelationId()
            }
        }
    }

    @Nested
    inner class CorrelationIdMdcSqsListenerAspect {
        private val testSqsQueue = MessageQueue<TestSqsMessage>("correlation-id-test-queue", queueMessagingTemplate)

        @BeforeEach
        fun setup() {
            TestLogAppender.reset()
            MDC.clear()
        }

        @Test
        fun `should add correlation-id header to SQS message given correlationId not on MDC and SQS message is posted to queue`() {
            // Given

            // When
            testSqsQueue.submit(TestSqsMessage())

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Test SQS listener successfully called",
                        Level.INFO
                    )
                ).hasAnyCorrelationId()
            }
        }

        @Test
        fun `should add MDC correlationId value to SQS message header given correlationId is set on MDC and sqs message is posted to queue`() {
            // Given
            val expectedCorrelationId = randomUUID().toString().replace("-", "")
            MDC.put("correlationId", expectedCorrelationId)

            // When
            testSqsQueue.submit(TestSqsMessage())

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Test SQS listener successfully called",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
            }
        }
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
            getRequestedFor(urlEqualTo("/external-rest-api"))
                .withHeader("x-correlation-id", correlationIdMatcher)
        )
    }
}
