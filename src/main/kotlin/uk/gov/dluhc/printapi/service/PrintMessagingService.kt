package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage

@Service
class PrintMessagingService(
    private val processPrintResponseFileQueue: MessageQueue<ProcessPrintResponseFileMessage>
) {

    fun submitPrintResponseFileForProcessing(messagePayload: ProcessPrintResponseFileMessage) {
        processPrintResponseFileQueue.submit(messagePayload)
    }
}
