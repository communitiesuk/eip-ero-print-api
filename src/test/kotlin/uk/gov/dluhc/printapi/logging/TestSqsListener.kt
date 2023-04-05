package uk.gov.dluhc.printapi.logging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.util.UUID

private val logger = KotlinLogging.logger {}

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
