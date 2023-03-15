package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService
import uk.gov.dluhc.printapi.service.TemporaryCertificateDataRetentionService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ApplicationRemovedMessage] messages
 */
@Component
class ApplicationRemovedMessageListener(
    val certificateDataRetentionService: CertificateDataRetentionService,
    val temporaryCertificateDataRetentionService: TemporaryCertificateDataRetentionService
) : MessageListener<ApplicationRemovedMessage> {

    @SqsListener("\${sqs.application-removed-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ApplicationRemovedMessage) {
        with(payload) {
            logger.info {
                "ApplicationRemovedMessage for application with source type [$sourceType] " +
                    "and source reference [$sourceReference] " +
                    "and gssCode [$gssCode]"
            }
            certificateDataRetentionService.handleSourceApplicationRemoved(payload)
            temporaryCertificateDataRetentionService.handleSourceApplicationRemoved(payload)
        }
    }
}
