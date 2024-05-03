package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage

private val logger = KotlinLogging.logger { }

@Component
class PrintRequestsService(
    private val processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>,
    private val certificateBatchingService: CertificateBatchingService
) {

    fun processPrintRequests() {
        logger.info { "Looking for certificate Print Requests to assign to a new batch" }

        // split into batches and save to database before sending messages to SQS
        // Statistics are intentionally not updated here as they will be updated when the batch is processed,
        // and sending up to 5000 SQS messages was significantly slowing the job down.
        certificateBatchingService.batchPendingCertificates()
            .onEach { (batchId, printRequestCount) ->
                processPrintRequestQueue.submit(ProcessPrintRequestBatchMessage(batchId, printRequestCount))
                logger.info { "Batch [$batchId] containing $printRequestCount print requests submitted to queue" }
            }

        logger.info { "Completed batching certificate Print Requests" }
    }
}
