package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintBatchStatusUpdateMessage
import uk.gov.dluhc.printapi.service.PrintResponseFileService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ProcessPrintBatchStatusUpdateMessage] messages
 */
@Component
class ProcessPrintResponseFileMessageListener(
    private val printResponseFileService: PrintResponseFileService
) : MessageListener<ProcessPrintBatchStatusUpdateMessage> {

    @SqsListener("\${sqs.process-print-response-file-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ProcessPrintBatchStatusUpdateMessage) {
        with(payload) {
            val filePath = "$fileDirectory/$fileName"
            logger.info { "Begin processing PrintResponse file [$filePath]" }
            val printResponses = printResponseFileService.fetchAndUnmarshallPrintResponses(filePath)
            printResponseFileService.processPrintResponses(printResponses)
            printResponseFileService.removeRemoteFile(filePath)
            logger.info { "Completed processing PrintResponse file [$filePath]" }
        }
    }
}
