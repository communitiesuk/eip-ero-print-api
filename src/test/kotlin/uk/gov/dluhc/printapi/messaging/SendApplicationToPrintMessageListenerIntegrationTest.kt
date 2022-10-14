package uk.gov.dluhc.printapi.messaging

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.util.concurrent.TimeUnit.SECONDS

internal class SendApplicationToPrintMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val payload = buildSendApplicationToPrintMessage()

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.atMost(5, SECONDS).untilAsserted {
            assertThat(
                TestLogAppender.hasLog(
                    "Sending application [${payload.sourceReference}] to print", Level.INFO
                )
            ).isTrue()
        }
    }

    @Test
    fun `should not process message that does conform to validation constraints`() {
        // Given
        val payload = buildSendApplicationToPrintMessage(gssCode = "ABC") // gssCode must be 9 characters

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.during(5, SECONDS).until {
            assertThat(
                TestLogAppender.hasLog(
                    "Sending application [${payload.sourceReference}] to print", Level.INFO
                )
            ).isFalse
            true
        }
    }
}
