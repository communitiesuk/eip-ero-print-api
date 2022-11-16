package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.exception.EmptyBatchException
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
    fun processBatch(batchId: String) {
        val certificates = certificateRepository.findByStatusAndPrintRequestsBatchId(ASSIGNED_TO_BATCH, batchId)
        if (certificates.isEmpty()) {
            throw EmptyBatchException(batchId, ASSIGNED_TO_BATCH)
        }
        val fileContents = printFileDetailsFactory.createFileDetailsFromCertificates(batchId, certificates)
        val sftpInputStream = sftpZipInputStreamProvider.createSftpInputStream(fileContents)
        val sftpFilename = filenameFactory.createZipFilename(batchId, certificates.size)
        sftpService.sendFile(sftpInputStream, sftpFilename)
        updateCertificates(certificates)
    }

    private fun updateCertificates(certificates: List<Certificate>) {
        certificates.forEach { certificate ->
            certificate.addStatus(Status.SENT_TO_PRINT_PROVIDER)
        }
    }
}
