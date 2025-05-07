package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Context
import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.CertificateSearchSummaryResults
import uk.gov.dluhc.printapi.models.CertificateSearchSummaryResponse

@Mapper(uses = [CertificateSummaryResponseMapper::class])
abstract class CertificateSummarySearchResponseMapper {
    abstract fun toCertificateSearchSummaryResponse(certificateSummaryResultsDto: CertificateSearchSummaryResults, @Context eroId: String): CertificateSearchSummaryResponse
}
