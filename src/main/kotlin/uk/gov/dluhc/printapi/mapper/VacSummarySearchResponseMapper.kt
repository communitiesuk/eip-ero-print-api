package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.dto.VacSearchSummaryResults
import uk.gov.dluhc.printapi.dto.VacSummaryDto
import uk.gov.dluhc.printapi.models.VacSearchSummaryResponse
import uk.gov.dluhc.printapi.models.VacSummaryResponse

@Mapper(uses = [PrintRequestStatusMapper::class, InstantMapper::class, DeliveryAddressTypeMapper::class])
abstract class VacSummarySearchResponseMapper {
    abstract fun toVacSearchSummaryResponse(vacSummaryResultsDto: VacSearchSummaryResults): VacSearchSummaryResponse

    @Mapping(source = "printRequests", target = "printRequestSummaries")
    protected abstract fun toVacSummaryResponse(dto: VacSummaryDto): VacSummaryResponse
}
