package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepositoryExtensions.findPendingRemoval
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage

private val logger = KotlinLogging.logger {}

@Service
class TemporaryCertificateDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val temporaryCertificateRepository: TemporaryCertificateRepository,
    private val electorDocumentRemovalDateResolver: ElectorDocumentRemovalDateResolver
) {

    /**
     * Sets the finalRetentionRemovalDate on a [uk.gov.dluhc.printapi.database.entity.TemporaryCertificate], after the
     * originating application is removed from the source system (e.g. VCA).
     *
     * @param message An [ApplicationRemovedMessage] sent from the source system.
     */
    @Transactional
    fun handleSourceApplicationRemoved(message: ApplicationRemovedMessage) {
        with(message) {
            val sourceType = sourceTypeMapper.mapSqsToEntity(sourceType)
            val temporaryCerts =
                temporaryCertificateRepository.findByGssCodeAndSourceTypeAndSourceReference(
                    gssCode = gssCode,
                    sourceType = sourceType,
                    sourceReference = sourceReference
                )
            if (temporaryCerts.isEmpty()) {
                logger.warn { "No Temporary Certificate with sourceType = $sourceType and sourceReference = $sourceReference found" }
            } else {
                temporaryCerts.forEach {
                    it.finalRetentionRemovalDate = electorDocumentRemovalDateResolver.getTempCertFinalRetentionPeriodRemovalDate(it.issueDate)
                    temporaryCertificateRepository.save(it)
                }
            }
        }
    }

    @Transactional
    fun removeTemporaryCertificateData(sourceType: SourceType) {
        with(temporaryCertificateRepository.findPendingRemoval(sourceType = sourceType)) {
            logger.info { "Found $size temporary certificates with sourceType $sourceType to remove" }
            forEach { temporaryCertificateRepository.deleteById(it.id!!) }
        }
    }
}
