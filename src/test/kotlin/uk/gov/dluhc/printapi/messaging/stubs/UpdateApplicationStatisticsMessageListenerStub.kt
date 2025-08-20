package uk.gov.dluhc.printapi.messaging.stubs

import io.awspring.cloud.sqs.annotation.SqsListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.applicationsapi.messaging.models.UpdateApplicationStatisticsMessage

@Component
class UpdateApplicationStatisticsMessageListenerStub : MessageListenerStub<UpdateApplicationStatisticsMessage>() {

    @SqsListener("trigger-application-statistics-update.fifo")
    override fun handleMessage(@Payload payload: UpdateApplicationStatisticsMessage) {
        super.handleMessage(payload)
    }
}
