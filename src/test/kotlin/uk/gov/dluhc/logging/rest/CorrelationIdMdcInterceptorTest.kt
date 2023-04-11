package uk.gov.dluhc.logging.rest

import ch.qos.logback.classic.Level
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import uk.gov.dluhc.logging.testsupport.assertj.assertions.ILoggingEventAssert
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAdminBearerToken
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class CorrelationIdMdcInterceptorTest : IntegrationTest() {
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
            ILoggingEventAssert.assertThat(
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
        val expectedCorrelationId = UUID.randomUUID().toString().replace("-", "")

        // When
        webTestClient.get()
            .uri(URI_TEMPLATE)
            .bearerToken(getVCAdminBearerToken())
            .header("x-correlation-id", expectedCorrelationId)
            .exchange()
            .expectHeader().valueEquals("x-correlation-id", expectedCorrelationId)

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            ILoggingEventAssert.assertThat(
                TestLogAppender.getLogEventMatchingRegex(
                    "Test API successfully called",
                    Level.INFO
                )
            ).hasCorrelationId(expectedCorrelationId)
        }
    }
}
