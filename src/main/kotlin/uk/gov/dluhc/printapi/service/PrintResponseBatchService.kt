package uk.gov.dluhc.printapi.service

import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import java.time.Clock
import java.time.OffsetDateTime

class PrintResponseBatchService(
    private val printDetailsRepository: PrintDetailsRepository,
    private val idFactory: IdFactory,
    private val clock: Clock
) {
    fun processBatchResponses(batchResponses: List<BatchResponse>) {
        batchResponses.forEach { batchResponse ->
            val printDetails =
                printDetailsRepository.getAllByStatusAndBatchId(SENT_TO_PRINT_PROVIDER, batchResponse.batchId)

            if (batchResponse.status == SUCCESS) {
                printDetails.forEach {
                    it.addStatus(
                        status = RECEIVED_BY_PRINT_PROVIDER,
                        dateCreated = OffsetDateTime.now(clock),
                        eventDateTime = batchResponse.timestamp,
                        message = null
                    )
                }
            } else {
                printDetails.forEach {
                    it.addStatus(
                        status = PENDING_ASSIGNMENT_TO_BATCH,
                        dateCreated = OffsetDateTime.now(clock),
                        eventDateTime = batchResponse.timestamp,
                        message = null
                    )
                    it.requestId = idFactory.requestId()
                }
            }

            printDetailsRepository.updateItems(printDetails)
        }
    }
}
