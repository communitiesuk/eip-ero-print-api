package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepositoryExtensions.findPendingRemoval
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.ApplicationRemovedMessage
import java.time.LocalDate
import java.time.Month

private val logger = KotlinLogging.logger {}

@Service
class TemporaryCertificateDataRetentionService(
    private val sourceTypeMapper: SourceTypeMapper,
    private val temporaryCertificateRepository: TemporaryCertificateRepository
) {

    /**
     * Sets the initialRetentionRemovalDate on a [uk.gov.dluhc.printapi.database.entity.TemporaryCertificate], after the
     * originating application is removed from the source system (e.g. VCA).
     *
     * @param message An [ApplicationRemovedMessage] sent from the source system.
     */
    @Transactional
    fun handleSourceApplicationRemoved(message: ApplicationRemovedMessage) {
        with(message) {
            val sourceType = sourceTypeMapper.mapSqsToEntity(sourceType)
            temporaryCertificateRepository.findByGssCodeAndSourceTypeAndSourceReference(
                gssCode = gssCode,
                sourceType = sourceType,
                sourceReference = sourceReference
            )?.also {
                it.finalRetentionRemovalDate = getFinalRetentionPeriodRemovalDate(it.issueDate)
                temporaryCertificateRepository.save(it)
            } ?: logger.error { "Temporary certificate with sourceType = $sourceType and sourceReference = $sourceReference not found" }
        }
    }

    @Transactional
    fun removeTemporaryCertificateData(sourceType: SourceType) {
        with(temporaryCertificateRepository.findPendingRemoval(sourceType = sourceType)) {
            logger.info { "Found $size temporary certificates with sourceType $sourceType to remove" }
            forEach { temporaryCertificateRepository.deleteById(it.id!!) }
        }
    }

    private fun getFinalRetentionPeriodRemovalDate(issueDate: LocalDate): LocalDate? {
        val firstJuly = LocalDate.of(issueDate.year, Month.JULY, 1)
        val numberOfYears =
            when (issueDate.isBefore(firstJuly)) {
                true -> 1L
                false -> 2L
            }
        return firstJuly.plusYears(numberOfYears)
    }
}
