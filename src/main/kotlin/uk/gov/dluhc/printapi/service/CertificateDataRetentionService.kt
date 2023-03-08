package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.database.repository.DeliveryRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import javax.transaction.Transactional

private val logger = KotlinLogging.logger {}

@Service
class CertificateDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val certificateRepository: CertificateRepository,
    private val deliveryRepository: DeliveryRepository,
    private val certificateRemovalDateResolver: CertificateRemovalDateResolver,
    private val s3CertificatePhotoService: S3CertificatePhotoService
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

    @Transactional
    fun removeFinalRetentionPeriodData(sourceType: SourceType) {
        logger.info { "Finding certificates with sourceType $sourceType to remove final retention period data from" }
        with(certificateRepository.findPendingRemovalOfFinalRetentionData(sourceType = sourceType)) {
            forEach {
                s3CertificatePhotoService.removeCertificatePhoto(it)
                certificateRepository.delete(it)
                logger.info { "Removed remaining data after final retention period from certificate with sourceReference ${it.sourceReference}" }
            }
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
