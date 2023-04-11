package uk.gov.dluhc.logging.config

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

/**
 * A simple SQS listener to be used by [CorrelationIdMdcMessageListenerAspectTest]
 */
@Component
class TestSqsListener {

    @SqsListener("correlation-id-test-queue")
    fun handleMessage(payload: TestSqsMessage) {
        logger.info { "Test SQS listener successfully called" }
    }
}

data class TestSqsMessage(
    val id: UUID = UUID.randomUUID()
)
