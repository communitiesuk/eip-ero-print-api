package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.service.ProcessPrintBatchService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

@Component
class ProcessPrintRequestBatchMessageListener(
    private val processPrintBatchService: ProcessPrintBatchService
) : MessageListener<ProcessPrintRequestBatchMessage> {

    @SqsListener("\${sqs.process-print-request-batch-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ProcessPrintRequestBatchMessage) {
        with(payload) {
            logger.info("Processing print batch request for batchId: $batchId")

            processPrintBatchService.processBatch(batchId)

            logger.info("Successfully processed print request for batchId: $batchId")
        }
    }
}
