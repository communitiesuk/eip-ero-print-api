package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse

@Mapper(uses = [PrintRequestStatusMapper::class, InstantMapper::class])
interface CertificateSummaryResponseMapper {
    @Mapping(source = "printRequests", target = "printRequestSummaries")
    fun toCertificateSummaryResponse(dto: CertificateSummaryDto): CertificateSummaryResponse
}
