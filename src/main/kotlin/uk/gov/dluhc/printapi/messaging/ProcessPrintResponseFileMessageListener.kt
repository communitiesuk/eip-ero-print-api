package uk.gov.dluhc.printapi.messaging

import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.service.SftpService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ProcessPrintResponseFileMessage] messages
 */
@Component
class ProcessPrintResponseFileMessageListener(
    private val sftpService: SftpService,
) : MessageListener<ProcessPrintResponseFileMessage> {

    @SqsListener("\${sqs.process-print-response-file-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ProcessPrintResponseFileMessage) {
        with(payload) {
            val filePath = "$directory/$fileName"
            logger.info { "Begin processing PrintResponse file [$filePath]" }
            val printResponsesString = sftpService.fetchFileFromOutBoundDirectory(payload.directory, payload.fileName)
            processPrintResponses(printResponsesString)
            sftpService.removeFileFromOutBoundDirectory(payload.directory, payload.fileName)
            logger.info { "Completed processing PrintResponse file [$filePath]" }
        }
    }

    private fun processPrintResponses(printResponses: String) {
        logger.info { "processing $printResponses" }
        // TODO in EIP1-2262
    }
}
