package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateStatisticsMessage
import java.util.UUID

@Service
class StatisticsUpdateService(
    private val triggerVoterCardStatisticsUpdateQueue: MessageQueue<UpdateStatisticsMessage>
) {
    fun triggerVoterCardStatisticsUpdate(applicationId: String) {
        triggerVoterCardStatisticsUpdateQueue.submit(
            UpdateStatisticsMessage(voterCardApplicationId = applicationId),
            mapOf(
                "message-group-id" to applicationId,
                "message-deduplication-id" to UUID.randomUUID().toString(),
            )
        )
    }
}
