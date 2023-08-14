package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.VacSearchCriteriaDto
import uk.gov.dluhc.printapi.rest.VacSearchQueryStringParameters

@Mapper
interface VacSearchQueryStringParametersMapper {

    fun toVacSearchCriteriaDto(
        eroId: String,
        searchQueryParameters: VacSearchQueryStringParameters
    ): VacSearchCriteriaDto
}
