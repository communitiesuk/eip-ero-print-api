package uk.gov.dluhc.logging.config

import ch.qos.logback.classic.Level
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.logging.testsupport.assertj.assertions.ILoggingEventAssert
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import java.util.concurrent.TimeUnit

internal class CorrelationIdMdcScheduledAspectTest : IntegrationTest() {

    @Autowired
    private lateinit var testScheduledJob: TestScheduledJob

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
            ILoggingEventAssert.assertThat(
                TestLogAppender.getLogEventMatchingRegex(
                    "Test scheduled job successfully called",
                    Level.INFO
                )
            ).hasAnyCorrelationId()
        }
    }
}
