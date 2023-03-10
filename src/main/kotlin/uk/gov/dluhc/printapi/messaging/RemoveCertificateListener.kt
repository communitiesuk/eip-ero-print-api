package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.printapi.service.CertificateDataRetentionService
import javax.validation.Valid

/**
 * Implementation of [MessageListener] to handle [RemoveCertificateMessage] messages
 */
@Component
class RemoveCertificateListener(
    val certificateDataRetentionService: CertificateDataRetentionService
) : MessageListener<RemoveCertificateMessage> {

    @SqsListener("\${sqs.remove-certificate-queue-name}")
    override fun handleMessage(@Valid @Payload payload: RemoveCertificateMessage) {
        certificateDataRetentionService.removeFinalRetentionPeriodData(payload)
    }
}
