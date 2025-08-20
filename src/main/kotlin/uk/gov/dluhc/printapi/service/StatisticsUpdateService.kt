package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.applicationsapi.messaging.models.UpdateApplicationStatisticsMessage
import uk.gov.dluhc.messagingsupport.MessageQueue
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
