package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository

/**
 * Processes a print batch request by streaming a zip file containing manifest and photo images
 * to printer SFTP destination.
 */
@Service
class ProcessPrintBatchService(
    private val printDetailsRepository: PrintDetailsRepository,
    private val printFileDetailsFactory: PrintFileDetailsFactory,
    private val sftpZipInputStreamProvider: SftpInputStreamProvider,
    private val filenameFactory: FilenameFactory,
    private val sftpService: SftpService
) {

    /**
     * Step 1: Listener receives request
     * Step 2: DynamoDB is queried for the batch being processed
     * Step 3: Create Zip & SFTP streams
     *
     * For Zip stream:
     * Step A: Add CSV file containing applications to print to stream
     * Step B: Add images from S3 to zip stream
     *
     * For SFTP stream:
     * Step A: Connect to SFTP destination directory and create temp file
     * Step B: Stream contents to SFTP
     * Step C: Rename the file stored on SFTP server
     *
     * Step 4: Update Dynamo batch records with new status and name of zip sent to printer
     */
    fun processBatch(batchId: String) {
        val printList = printDetailsRepository.getAllByStatusAndBatchId(PENDING_ASSIGNMENT_TO_BATCH, batchId)
        val fileContents = printFileDetailsFactory.createFileDetails(batchId, printList)
        val sftpInputStream = sftpZipInputStreamProvider.createSftpInputStream(fileContents)
        val sftpFilename = filenameFactory.createZipFilename(batchId, printList.size)
        sftpService.sendFile(sftpInputStream, sftpFilename)
        printDetailsRepository.updateItems(updateBatch(printList))
    }

    private fun updateBatch(printList: List<PrintDetails>): List<PrintDetails> {
        printList.forEach { printDetails -> printDetails.status = Status.SENT_TO_PRINT_PROVIDER }
        return printList
    }
}
