package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import javax.transaction.Transactional

private val logger = KotlinLogging.logger {}

@Service
class CertificateDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val certificateRepository: CertificateRepository,
    private val certificateRemovalDateResolver: CertificateRemovalDateResolver
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
                it.initialRetentionRemovalDate = certificateRemovalDateResolver
                    .getCertificateInitialRetentionPeriodRemovalDate(it.issueDate, gssCode)
                certificateRepository.save(it)
            }
                ?: logger.error { "Certificate with sourceType = $sourceType and sourceReference = $sourceReference not found" }
        }
    }
}