package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.mapper.CertificateSummaryDtoMapper
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto

@Service
class CertificateSummaryService(
    private val certificateFinderService: CertificateFinderService,
    private val mapper: CertificateSummaryDtoMapper,
) {

    fun getCertificateSummary(eroId: String, sourceType: SourceType, sourceReference: String): CertificateSummaryDto {
        val certificate = certificateFinderService.getCertificate(eroId, sourceType, sourceReference)
        return mapper.certificateToCertificatePrintRequestSummaryDto(certificate)
    }
}
