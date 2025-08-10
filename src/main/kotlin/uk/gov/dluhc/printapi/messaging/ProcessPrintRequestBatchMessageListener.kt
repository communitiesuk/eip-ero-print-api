package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.sqs.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageListener
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.messaging.service.ProcessPrintBatchService
import uk.gov.dluhc.printapi.service.StatisticsUpdateService

private val logger = KotlinLogging.logger { }

@Component
class ProcessPrintRequestBatchMessageListener(
    private val processPrintBatchService: ProcessPrintBatchService,
    private val statisticsUpdateService: StatisticsUpdateService,
) : MessageListener<ProcessPrintRequestBatchMessage> {

    @SqsListener("\${sqs.process-print-request-batch-queue-name}")
    override fun handleMessage(@Payload payload: ProcessPrintRequestBatchMessage) {
        with(payload) {
            logger.info("Processing print batch request for batchId: $batchId")

            val certificates = processPrintBatchService.processBatch(batchId, printRequestCount)
            certificates.forEach {
                statisticsUpdateService.updateStatistics(it.sourceReference!!)
            }

            logger.info("Successfully processed print request for batchId: $batchId")
        }
    }
}
