package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageListener
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.service.PrintService
import uk.gov.dluhc.printapi.service.StatisticsUpdateService

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [SendApplicationToPrintMessage] messages
 */
@Component
class SendApplicationToPrintMessageListener(
    private val printService: PrintService,
    private val statisticsUpdateService: StatisticsUpdateService,
) : MessageListener<SendApplicationToPrintMessage> {

    @SqsListener("\${sqs.send-application-to-print-queue-name}")
    override fun handleMessage(@Payload payload: SendApplicationToPrintMessage) {
        with(payload) {
            logger.info { "Print message with source reference [$sourceReference] received" }
            printService.savePrintMessage(payload).also {
                if (payload.isFromApplicationsApi == true)
                    statisticsUpdateService.triggerApplicationStatisticsUpdate(it.sourceReference!!)
                else
                    statisticsUpdateService.triggerVoterCardStatisticsUpdate(it.sourceReference!!)
            }
            logger.info { "Print message with source reference [$sourceReference] saved" }
        }
    }
}
