package uk.gov.dluhc.printapi.service

import uk.gov.dluhc.printapi.database.entity.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.Status.NOT_DELIVERED
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.PRINT_PROVIDER_DISPATCH_FAILED
import uk.gov.dluhc.printapi.database.entity.Status.PRINT_PROVIDER_PRODUCTION_FAILED
import uk.gov.dluhc.printapi.database.entity.Status.PRINT_PROVIDER_VALIDATION_FAILED
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.VALIDATED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import java.time.Clock
import java.time.OffsetDateTime
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.Status as ResponseStatus
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep as ResponseStatusStep

class PrintResponseProcessingService(
    private val printDetailsRepository: PrintDetailsRepository,
    private val idFactory: IdFactory,
    private val clock: Clock
) {
    /**
     * Updates print details for the batches based on the batch responses' statuses.
     * If a batch is successful, a new printRequestStatus will be added for each corresponding print details with status
     * being RECEIVED_BY_PRINT_PROVIDER.
     *
     * If a batch is not successful, the status will be set to SENT_TO_PRINT_PROVIDER and a new requestId will be set.
     * The reason for the new requestId is, the print provider does not support duplicate print requests with the same requestId.
     *
     * dateCreated will be the timestamp the new printRequestStatus is created and eventDateTime will be the timestamp from
     * the print provider's batch response.
     */
    fun processBatchResponses(batchResponses: List<BatchResponse>) {
        batchResponses.forEach { batchResponse ->
            with(batchResponse) {
                val newStatus =
                    if (status == SUCCESS) RECEIVED_BY_PRINT_PROVIDER else PENDING_ASSIGNMENT_TO_BATCH

                val printDetails =
                    printDetailsRepository.getAllByStatusAndBatchId(SENT_TO_PRINT_PROVIDER, batchId)

                printDetails.forEach {
                    it.addStatus(
                        status = newStatus,
                        dateCreated = OffsetDateTime.now(clock),
                        eventDateTime = timestamp,
                        message = null
                    )

                    if (status != SUCCESS) {
                        it.requestId = idFactory.requestId()
                    }
                }

                printDetailsRepository.updateItems(printDetails)
            }
        }
    }

    fun processPrintResponse(printResponse: ProcessPrintResponseMessage) {
        val newStatus = with(printResponse) {
            when {
                statusStep == ResponseStatusStep.PROCESSED && status == ResponseStatus.SUCCESS -> VALIDATED_BY_PRINT_PROVIDER
                statusStep == ResponseStatusStep.IN_MINUS_PRODUCTION && status == ResponseStatus.SUCCESS -> IN_PRODUCTION
                statusStep == ResponseStatusStep.DISPATCHED && status == ResponseStatus.SUCCESS -> DISPATCHED
                statusStep == ResponseStatusStep.NOT_MINUS_DELIVERED && status == ResponseStatus.FAILED -> NOT_DELIVERED
                statusStep == ResponseStatusStep.PROCESSED && status == ResponseStatus.FAILED -> PRINT_PROVIDER_VALIDATION_FAILED
                statusStep == ResponseStatusStep.IN_MINUS_PRODUCTION && status == ResponseStatus.FAILED -> PRINT_PROVIDER_PRODUCTION_FAILED
                statusStep == ResponseStatusStep.DISPATCHED && status == ResponseStatus.FAILED -> PRINT_PROVIDER_DISPATCH_FAILED
                else -> throw IllegalArgumentException("Undefined statusStep [$statusStep] and status [$status] combination")
            }
        }

        val printDetails = printDetailsRepository.getByRequestId(printResponse.requestId)

        with(printResponse) {
            printDetails.addStatus(
                status = newStatus,
                dateCreated = OffsetDateTime.now(clock),
                eventDateTime = timestamp,
                message = message
            )
        }

        printDetailsRepository.updateItems(listOf(printDetails))
    }
}
