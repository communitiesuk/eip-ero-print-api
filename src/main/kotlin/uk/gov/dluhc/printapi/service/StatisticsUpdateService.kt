package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateApplicationStatisticsMessage
import uk.gov.dluhc.votercardapplicationsapi.messaging.models.UpdateStatisticsMessage
import java.util.UUID

@Service
class StatisticsUpdateService(
    private val triggerVoterCardStatisticsUpdateQueue: MessageQueue<UpdateStatisticsMessage>,
    private val triggerApplicationStatisticsUpdateQueue: MessageQueue<UpdateApplicationStatisticsMessage>,
) {

    fun updateStatistics(applicationId: String) {
        triggerApplicationStatisticsUpdate(applicationId)
    }

    fun triggerVoterCardStatisticsUpdate(applicationId: String) {
        triggerVoterCardStatisticsUpdateQueue.submit(
            UpdateStatisticsMessage(voterCardApplicationId = applicationId),
            mapOf(
                "message-group-id" to applicationId,
                "message-deduplication-id" to UUID.randomUUID().toString(),
            )
        )
    }

    fun triggerApplicationStatisticsUpdate(applicationId: String) {
        triggerApplicationStatisticsUpdateQueue.submit(
            UpdateApplicationStatisticsMessage(applicationId = applicationId),
            mapOf(
                "message-group-id" to applicationId,
                "message-deduplication-id" to UUID.randomUUID().toString(),
            )
        )
    }
}
