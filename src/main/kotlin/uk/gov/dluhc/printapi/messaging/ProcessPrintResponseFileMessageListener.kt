package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageListener
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.messaging.service.PrintResponseFileService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ProcessPrintResponseFileMessage] messages
 */
@Component
class ProcessPrintResponseFileMessageListener(
    private val printResponseFileService: PrintResponseFileService,
) : MessageListener<ProcessPrintResponseFileMessage> {

    @SqsListener("\${sqs.process-print-response-file-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ProcessPrintResponseFileMessage) {
        with(payload) {
            logger.info { "Begin processing PrintResponse file [$fileName] from directory [$directory]" }
            printResponseFileService.processPrintResponseFile(directory, fileName, payload.isFromApplicationsApi)
            logger.info { "Completed processing PrintResponse file [$fileName] from directory [$directory]" }
        }
    }
}
