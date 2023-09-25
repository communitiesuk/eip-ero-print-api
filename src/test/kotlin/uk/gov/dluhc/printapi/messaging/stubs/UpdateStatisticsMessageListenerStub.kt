package uk.gov.dluhc.printapi.messaging.stubs

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateStatisticsMessage
import javax.validation.Valid

@Component
class UpdateStatisticsMessageListenerStub : MessageListenerStub<UpdateStatisticsMessage>() {

    @SqsListener("trigger-voter-card-statistics-update.fifo")
    override fun handleMessage(@Valid @Payload payload: UpdateStatisticsMessage) {
        super.handleMessage(payload)
    }
}
