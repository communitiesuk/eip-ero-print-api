package uk.gov.dluhc.printapi.aspect

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.dluhc.printapi.service.StatisticsUpdateService

/**
 * Listens for StatistcsUpdateEvent events and sends a message to SQS to trigger a full statistics update.
 *
 * This listener is a TransactionEventListener, not just an EventListener, because it is important that any changes
 * that triggered a statistics update are committed before we request a stats update.
 *
 * Stats updates are triggered from the StatisticsUpdateAspect, which contains advice which runs after `save` or
 * `saveAll` is called on a JpaRepository, which will be within a transaction. The advice will run in the same
 * transaction as the `save` call, and in particular will run before that transaction is committed. This leads to two
 * scenarios we would like to avoid:
 *
 *  1) The stats update completes before the transaction is committed. In this case, the stats update will be reading
 *     stale data from Print API.
 *
 *  2) The transaction fails to commit, and we end up carrying out a stats update anyway. This is harmless but still
 *     not ideal.
 *
 * We avoid both of these scenarios by publishing an event from the StatisticsUpdateAspect, and capturing that event
 * here in a TransactionalEventListener, which will only execute once the transaction which published the event has
 * committed.
 */
@Component
class StatisticsUpdateEventListener(
    private val statisticsUpdateService: StatisticsUpdateService
) {

    @TransactionalEventListener
    fun updateStatistics(event: StatisticsUpdateEvent) {
        statisticsUpdateService.triggerVoterCardStatisticsUpdate(event.applicationId)
    }
}
