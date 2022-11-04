package uk.gov.dluhc.printapi.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.messaging.listener.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.service.SftpService
import javax.validation.Valid

private val logger = KotlinLogging.logger { }

/**
 * Implementation of [MessageListener] to handle [ProcessPrintResponseFileMessage] messages
 */
@Component
class ProcessPrintResponseFileMessageListener(
    private val sftpService: SftpService,
    private val objectMapper: ObjectMapper,
) : MessageListener<ProcessPrintResponseFileMessage> {

    @SqsListener("\${sqs.process-print-response-file-queue-name}")
    override fun handleMessage(@Valid @Payload payload: ProcessPrintResponseFileMessage) {
        with(payload) {
            val filePath = "$directory/$fileName"
            logger.info { "Begin processing PrintResponse file [$filePath]" }
            val printResponsesString = sftpService.fetchFile(filePath)
            val printResponses = objectMapper.readValue(printResponsesString, PrintResponses::class.java)
            processPrintResponses(printResponses)
            sftpService.removeFileFromOutBoundDirectory(filePath)
            logger.info { "Completed processing PrintResponse file [$filePath]" }
        }
    }

    private fun processPrintResponses(printResponses: PrintResponses) {
        logger.info { "processing $printResponses" }
        // TODO in subsequent jira ticket
    }
}
