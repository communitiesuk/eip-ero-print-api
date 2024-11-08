package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageListener
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import uk.gov.dluhc.printapi.messaging.models.SourceType.ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT
import uk.gov.dluhc.printapi.messaging.models.SourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.printapi.service.AedDataRetentionService
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService
import uk.gov.dluhc.printapi.service.TemporaryCertificateDataRetentionService

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ApplicationRemovedMessage] messages
 */
@Component
class ApplicationRemovedMessageListener(
    private val certificateDataRetentionService: CertificateDataRetentionService,
    private val temporaryCertificateDataRetentionService: TemporaryCertificateDataRetentionService,
    private val aedDataRetentionService: AedDataRetentionService
) : MessageListener<ApplicationRemovedMessage> {

    @SqsListener("\${sqs.application-removed-queue-name}")
    override fun handleMessage(@Payload payload: ApplicationRemovedMessage) {
        with(payload) {
            logger.info { "ApplicationRemovedMessage for application with source type [$sourceType] and source reference [$sourceReference] and gssCode [$gssCode]" }
            when (sourceType) {
                ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT -> aedDataRetentionService.handleSourceApplicationRemoved(payload)
                VOTER_MINUS_CARD -> {
                    temporaryCertificateDataRetentionService.handleSourceApplicationRemoved(payload)
                    certificateDataRetentionService.handleSourceApplicationRemoved(payload)
                }
            }
        }
    }
}
