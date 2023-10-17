package uk.gov.dluhc.printapi.aspect

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.dluhc.printapi.service.StatisticsUpdateService

@Component
class StatisticsUpdateEventListener(
    private val statisticsUpdateService: StatisticsUpdateService
) {

    @TransactionalEventListener
    fun updateStatistics(event: StatisticsUpdateEvent) {
        statisticsUpdateService.triggerVoterCardStatisticsUpdate(event.applicationId)
    }
}
