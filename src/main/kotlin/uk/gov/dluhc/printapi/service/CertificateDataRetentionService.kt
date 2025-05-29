package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage

private val logger = KotlinLogging.logger {}

@Service
class CertificateDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val certificateRepository: CertificateRepository,
    private val removalDateResolver: ElectorDocumentRemovalDateResolver,
    private val s3CertificatePhotoService: S3AccessService,
    private val removeCertificateQueue: MessageQueue<RemoveCertificateMessage>,
    private val dataRetentionConfiguration: DataRetentionConfiguration
) {

    /**
     * Sets the initialRetentionRemovalDate on a [uk.gov.dluhc.printapi.database.entity.Certificate], after the
     * originating application is removed from the source system (e.g. VCA).
     *
     * @param message An [ApplicationRemovedMessage] sent from the source system.
     */
    @Transactional
    fun handleSourceApplicationRemoved(message: ApplicationRemovedMessage) {
        val sourceType = sourceTypeMapper.mapSqsToEntity(message.sourceType)
        val certificate = certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(
            gssCode = message.gssCode,
            sourceType = sourceType,
            sourceReference = message.sourceReference,
        ) ?: run {
            logger.error { "Certificate with sourceType = $sourceType and sourceReference = ${message.sourceReference} not found" }
            return
        }

        try {
            setCertificateRetentionRemovalDates(certificate, message.gssCode)
        } catch (_: IllegalArgumentException) {
            // EROPSPT-418: We expect that it's very unlikely for this to happen.
            // The retention dates will be set when the issue date is set.
            // Logging a warning here to record instances of it.
            logger.warn { "No issue date found for certificate with sourceType = $sourceType and sourceReference = ${message.sourceReference}" }
        }

        certificate.hasSourceApplicationBeenRemoved = true
        certificateRepository.save(certificate)
    }

    fun setCertificateRetentionRemovalDates(certificate: Certificate, gssCode: String): Certificate {
        val issueDate = certificate.issueDate

        if (issueDate == null) {
            throw IllegalArgumentException("Issue date not found for certificate")
        }

        return certificate.also {
            it.initialRetentionRemovalDate =
                removalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(
                    issueDate,
                    gssCode,
                    it.isCertificateCreatedWithPrinterProvidedIssueDate ?: false
                )
            it.finalRetentionRemovalDate =
                removalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(issueDate)
        }
    }

    @Transactional
    fun removeInitialRetentionPeriodData(sourceType: SourceType) {
        logger.info { "Finding certificates with sourceType $sourceType to remove initial retention period data from" }
        with(certificateRepository.findPendingRemovalOfInitialRetentionData(sourceType = sourceType)) {
            forEach {
                removeInitialRetentionPeriodData(it.printRequests)
                it.initialRetentionDataRemoved = true
                certificateRepository.save(it)
                logger.info { "Removed initial retention period data from certificate with sourceReference ${it.sourceReference}" }
            }
        }
    }

    /**
     * Finds Certificates that are due for removal and places each one on a queue to be processed separately. The data
     * that is kept on the Certificate itself must be kept until the tenth 1st July (so nearly 10 years), which means
     * there could be a very large number to remove on the 1st July. Because of this, this method retrieves the required
     * "identifiers" (namely the Certificate ID and Photo S3 arn) in batches of 10,000 at a time.
     */
    @Transactional(readOnly = true)
    fun queueCertificatesForRemoval(sourceType: SourceType) {
        // initial query to get the number of records
        val totalElements =
            certificateRepository.countBySourceTypeAndFinalRetentionRemovalDateBefore(sourceType = sourceType)
        if (totalElements == 0) {
            logger.info { "No certificates with sourceType $sourceType to remove final retention period data from" }
            return
        }

        logger.info { "Found $totalElements certificates with sourceType $sourceType to remove" }
        val batchSize = dataRetentionConfiguration.certificateRemovalBatchSize
        val totalBatches = calculateTotalBatches(totalElements, batchSize)

        // items are removed as we go through them (via the SQS queue), so start at the last batch
        var batchNumber = totalBatches
        while (batchNumber > 0) {
            logger.info { "Retrieving batch [$batchNumber] of [$totalBatches] of CertificateRemovalSummary" }
            with(
                certificateRepository.findPendingRemovalOfFinalRetentionData(
                    sourceType = sourceType,
                    batchNumber = batchNumber,
                    batchSize = batchSize
                )
            ) {
                forEach { removeCertificateQueue.submit(RemoveCertificateMessage(it.id!!, it.photoLocationArn!!)) }
                batchNumber--
            }
        }
    }

    /**
     * Removes any remaining Certificate data from the database, as well as its photo in S3. This method processes each
     * message placed on the `removeCertificateQueue` in `removeFinalRetentionPeriodData()` above.
     */
    @Transactional
    fun removeFinalRetentionPeriodData(message: RemoveCertificateMessage) {
        with(message) {
            s3CertificatePhotoService.removeDocument(certificatePhotoArn)
            certificateRepository.deleteById(certificateId)
        }
    }

    private fun removeInitialRetentionPeriodData(printRequests: List<PrintRequest>) {
        printRequests.forEach {
            it.delivery = null
            it.supportingInformationFormat = null
        }
    }

    private fun calculateTotalBatches(totalElements: Int, batchSize: Int): Int {
        val totalBatches = (totalElements / batchSize)
        val remainder = if (totalElements % batchSize == 0) 0 else 1
        return totalBatches + remainder
    }
}
