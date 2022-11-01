package uk.gov.dluhc.printapi.service

import org.springframework.beans.factory.annotation.Value
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository

class PrintRequestsService(private val printDetailsRepository: PrintDetailsRepository, private val idFactory: IdFactory) {

    fun processPrintRequests(@Value("\${jobs.print-requests.batchSize}") batchSize: Int) {
        val batches = batchPrintRequests(batchSize)
        batches.map { (batchId, printDetails) ->
            printDetails.map { it.copy(status = Status.ASSIGNED_TO_BATCH, batchId = batchId) }
        }.flatten().forEach { printDetailsRepository.save(it) }
    }

    fun batchPrintRequests(batchSize: Int): Map<String, List<PrintDetails>> {
        val printDetailsPendingAssignment = printDetailsRepository.getAllByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        return printDetailsPendingAssignment.chunked(batchSize).associateBy { idFactory.batchId() }
    }
}
