package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateApplicationStatisticsMessage
import java.util.UUID

@Service
class StatisticsUpdateService(
    private val triggerApplicationStatisticsUpdateQueue: MessageQueue<UpdateApplicationStatisticsMessage>,
) {
    fun triggerApplicationStatisticsUpdate(applicationId: String) {
        triggerApplicationStatisticsUpdateQueue.submit(
            UpdateApplicationStatisticsMessage(externalId = applicationId),
            mapOf(
                "message-group-id" to applicationId,
                "message-deduplication-id" to UUID.randomUUID().toString(),
            )
        )
    }
}
