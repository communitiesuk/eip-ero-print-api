package uk.gov.dluhc.printapi.messaging.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findDistinctByPrintRequestStatusAndBatchId
import uk.gov.dluhc.printapi.exception.InsufficientPrintRequestsInBatchException
import uk.gov.dluhc.printapi.service.FilenameFactory
import uk.gov.dluhc.printapi.service.PrintFileDetailsFactory
import uk.gov.dluhc.printapi.service.SftpInputStreamProvider
import uk.gov.dluhc.printapi.service.SftpService
import uk.gov.dluhc.printapi.service.countPrintRequestsAssignedToBatch
import javax.transaction.Transactional

/**
 * Processes a print batch request by streaming a zip file containing manifest and photo images
 * to printer SFTP destination.
 */
@Service
class ProcessPrintBatchService(
    private val printFileDetailsFactory: PrintFileDetailsFactory,
    private val certificateRepository: CertificateRepository,
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
     * Step 4: Update Dynamo batch records with new status
     */
    @Transactional
    fun processBatch(batchId: String, printRequestCount: Int?) {
        val certificates = certificateRepository.findDistinctByPrintRequestStatusAndBatchId(ASSIGNED_TO_BATCH, batchId)
        verifyPrintRequestCount(certificates, batchId, printRequestCount)
        val fileContents = printFileDetailsFactory.createFileDetailsFromCertificates(batchId, certificates)
        val sftpInputStream = sftpZipInputStreamProvider.createSftpInputStream(fileContents)
        val sftpFilename = filenameFactory.createZipFilename(batchId, certificates)
        sftpService.sendFile(sftpInputStream, sftpFilename)
        updateCertificates(batchId, certificates)
    }

    private fun verifyPrintRequestCount(certificates: List<Certificate>, batchId: String, expectedCount: Int?) {
        if (certificates.isEmpty()) {
            throw InsufficientPrintRequestsInBatchException(batchId, ASSIGNED_TO_BATCH, 0, expectedCount)
        } else if (expectedCount != null) {
            countPrintRequestsAssignedToBatch(certificates, batchId).let {
                if (it != expectedCount) {
                    throw InsufficientPrintRequestsInBatchException(batchId, ASSIGNED_TO_BATCH, it, expectedCount)
                }
            }
        }
    }

    private fun updateCertificates(batchId: String, certificates: List<Certificate>) {
        certificates.forEach { certificate -> certificate.addSentToPrintProviderEventForBatch(batchId) }
    }
}
