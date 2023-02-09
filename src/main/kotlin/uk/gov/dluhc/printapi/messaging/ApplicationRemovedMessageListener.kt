package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ApplicationRemovedMessage] messages
 */
@Component
class ApplicationRemovedMessageListener() :
    MessageListener<ApplicationRemovedMessage> {

    @SqsListener("\${sqs.source-application-removed-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ApplicationRemovedMessage) {
        with(payload) {
            logger.info { "ApplicationRemovedMessage for application with source reference [$sourceReference]" }
        }
    }
}
