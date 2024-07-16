package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageListener
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService

/**
 * Implementation of [MessageListener] to handle [RemoveCertificateMessage] messages
 */
@Component
class RemoveCertificateListener(
    val certificateDataRetentionService: CertificateDataRetentionService
) : MessageListener<RemoveCertificateMessage> {

    @SqsListener("\${sqs.remove-certificate-queue-name}")
    override fun handleMessage(@Payload payload: RemoveCertificateMessage) {
        certificateDataRetentionService.removeFinalRetentionPeriodData(payload)
    }
}
