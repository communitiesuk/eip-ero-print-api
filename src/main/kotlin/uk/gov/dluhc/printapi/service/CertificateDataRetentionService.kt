package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.database.repository.DeliveryRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage

private val logger = KotlinLogging.logger {}

@Service
class CertificateDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val certificateRepository: CertificateRepository,
    private val deliveryRepository: DeliveryRepository,
    private val certificateRemovalDateResolver: CertificateRemovalDateResolver,
    private val s3CertificatePhotoService: S3PhotoService,
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
        with(message) {
            val sourceType = sourceTypeMapper.mapSqsToEntity(sourceType)
            certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(
                gssCode = gssCode,
                sourceType = sourceType,
                sourceReference = sourceReference
            )?.also {
                it.initialRetentionRemovalDate = certificateRemovalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(it.issueDate, gssCode)
                it.finalRetentionRemovalDate = certificateRemovalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(it.issueDate)
                certificateRepository.save(it)
            }
                ?: logger.error { "Certificate with sourceType = $sourceType and sourceReference = $sourceReference not found" }
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
        val batchSize = dataRetentionConfiguration.certificateRemovalBatchSize
        val batchedResults = certificateRepository.findPendingRemovalOfFinalRetentionData(
            sourceType = sourceType,
            batchIndex = 0,
            batchSize = batchSize
        )
        if (batchedResults.isEmpty) {
            logger.info { "No certificates with sourceType $sourceType to remove final retention period data from" }
            return
        }

        logger.info { "Found ${batchedResults.totalElements} certificates with sourceType $sourceType to remove final retention period data from" }
        // items are removed as we go through them (via the SQS queue), so start at the last batch
        var batchIndex = batchedResults.totalPages - 1
        while (batchIndex >= 0) {
            with(certificateRepository.findPendingRemovalOfFinalRetentionData(sourceType = sourceType, batchIndex = batchIndex, batchSize = batchSize)) {
                forEach { removeCertificateQueue.submit(RemoveCertificateMessage(it.id!!, it.applicationReference!!)) } // TODO EIP1-4307 - change to photoLocationArn
                batchIndex--
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
            s3CertificatePhotoService.removePhoto(certificatePhotoArn)
            certificateRepository.deleteById(certificateId)
        }
    }

    private fun removeInitialRetentionPeriodData(printRequests: List<PrintRequest>) {
        printRequests.forEach {
            it.delivery?.let { delivery -> deliveryRepository.delete(delivery) }
            it.delivery = null
            it.supportingInformationFormat = null
        }
    }
}
