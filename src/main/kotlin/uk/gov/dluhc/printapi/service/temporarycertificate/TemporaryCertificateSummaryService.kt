package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.dto.TemporaryCertificateSummaryDto
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateSummaryMapper

@Service
class TemporaryCertificateSummaryService(
    private val temporaryCertificateFinderService: TemporaryCertificateFinderService,
    private val mapper: TemporaryCertificateSummaryMapper
) {

    fun getTemporaryCertificateSummaries(eroId: String, sourceType: SourceType, sourceReference: String): List<TemporaryCertificateSummaryDto> {
        val temporaryCertificates = temporaryCertificateFinderService.getTemporaryCertificates(
            eroId,
            sourceType,
            sourceReference
        )
        return temporaryCertificates.map {
            mapper.toDtoTemporaryCertificateSummary(it)
        }
    }
}
