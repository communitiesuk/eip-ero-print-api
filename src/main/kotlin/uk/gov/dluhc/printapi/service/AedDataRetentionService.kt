package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.AddressRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.database.repository.DeliveryRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage

private val logger = KotlinLogging.logger {}

@Service
class AedDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository,
    private val deliveryRepository: DeliveryRepository,
    private val addressRepository: AddressRepository,
    private val removalDateResolver: ElectorDocumentRemovalDateResolver,
    private val s3PhotoService: S3PhotoService,
) {

    /**
     * Sets the finalRetentionRemovalDate on a [uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument], after
     * the originating application is removed from the source system (e.g. VCA).
     *
     * @param message An [ApplicationRemovedMessage] sent from the source system.
     */
    @Transactional
    fun handleSourceApplicationRemoved(message: ApplicationRemovedMessage) {
        with(message) {
            val sourceType = sourceTypeMapper.mapSqsToEntity(sourceType)
            anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
                gssCode = gssCode,
                sourceType = sourceType,
                sourceReference = sourceReference
            )?.also {
                it.initialRetentionRemovalDate = removalDateResolver.getAedInitialRetentionPeriodRemovalDate(it.issueDate)
                it.finalRetentionRemovalDate = removalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(it.issueDate)
                anonymousElectorDocumentRepository.save(it)
            } ?: logger.error { "Anonymous Elector Document with sourceType = $sourceType and sourceReference = $sourceReference not found" }
        }
    }

    @Transactional
    fun removeInitialRetentionPeriodData(sourceType: SourceType) {
        logger.info { "Finding anonymous elector documents with sourceType $sourceType to remove initial retention period data from" }
        with(anonymousElectorDocumentRepository.findPendingRemovalOfInitialRetentionData(sourceType = sourceType)) {
            forEach {
                it.removeInitialRetentionPeriodData()
                anonymousElectorDocumentRepository.save(it)
                logger.info { "Removed initial retention period data from anonymous elector document with sourceReference ${it.sourceReference}" }
            }
        }
    }

    @Transactional
    fun removeFinalRetentionPeriodData(sourceType: SourceType) {
        with(anonymousElectorDocumentRepository.findPendingRemovalOfFinalRetentionData(sourceType = sourceType)) {
            logger.info { "Found $size Anonymous Elector Documents with sourceType $sourceType to remove" }
            forEach {
                s3PhotoService.removePhoto(it.photoLocationArn)
                anonymousElectorDocumentRepository.deleteById(it.id!!)
            }
        }
    }

    fun AnonymousElectorDocument.removeInitialRetentionPeriodData() {
        contactDetails?.email = null
        contactDetails?.phoneNumber = null
        addressRepository.deleteById(contactDetails?.address?.id!!)
        contactDetails?.address = null

        deliveryRepository.deleteById(delivery?.id!!)
        delivery = null
        supportingInformationFormat = null
        initialRetentionDataRemoved = true
    }
}
