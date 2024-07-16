package uk.gov.dluhc.printapi.messaging.stubs

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateStatisticsMessage

@Component
class UpdateStatisticsMessageListenerStub : MessageListenerStub<UpdateStatisticsMessage>() {

    @SqsListener("trigger-voter-card-statistics-update.fifo")
    override fun handleMessage(@Payload payload: UpdateStatisticsMessage) {
        super.handleMessage(payload)
    }
}
