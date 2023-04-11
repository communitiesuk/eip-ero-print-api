package uk.gov.dluhc.logging.config

import ch.qos.logback.classic.Level
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.logging.testsupport.assertj.assertions.ILoggingEventAssert
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class CorrelationIdMdcMessageListenerAspectTest : IntegrationTest() {

    @Autowired
    private lateinit var queueMessagingTemplate: QueueMessagingTemplate

    private lateinit var testSqsQueue: MessageQueue<TestSqsMessage>

    @BeforeEach
    fun setup() {
        TestLogAppender.reset()
        MDC.clear()
        testSqsQueue = MessageQueue("correlation-id-test-queue", queueMessagingTemplate)
    }

    @Test
    fun `should add correlation-id header to SQS message given correlationId not on MDC and SQS message is posted to queue`() {
        // Given

        // When
        testSqsQueue.submit(TestSqsMessage())

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            ILoggingEventAssert.assertThat(
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
        val expectedCorrelationId = UUID.randomUUID().toString().replace("-", "")
        MDC.put("correlationId", expectedCorrelationId)

        // When
        testSqsQueue.submit(TestSqsMessage())

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            ILoggingEventAssert.assertThat(
                TestLogAppender.getLogEventMatchingRegex(
                    "Test SQS listener successfully called",
                    Level.INFO
                )
            ).hasCorrelationId(expectedCorrelationId)
        }
    }
}
