package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage

private val logger = KotlinLogging.logger {}

@Service
class AedDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository,
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
                it.finalRetentionRemovalDate = removalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(it.issueDate)
                anonymousElectorDocumentRepository.save(it)
            } ?: logger.error { "Anonymous Elector Document with sourceType = $sourceType and sourceReference = $sourceReference not found" }
        }
    }

    @Transactional
    fun removeAnonymousElectorDocumentData(sourceType: SourceType) {
        with(anonymousElectorDocumentRepository.findPendingRemovalOfFinalRetentionData(sourceType = sourceType)) {
            logger.info { "Found $size Anonymous Elector Documents with sourceType $sourceType to remove" }
            forEach {
                s3PhotoService.removePhoto(it.photoLocationArn)
                anonymousElectorDocumentRepository.deleteById(it.id!!)
            }
        }
    }
}
