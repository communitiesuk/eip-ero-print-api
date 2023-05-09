package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.rest.aed.AedSearchQueryStringParameters

@Mapper
interface AedSearchQueryStringParametersMapper {

    fun toAnonymousSearchCriteriaDto(
        eroId: String,
        searchQueryParameters: AedSearchQueryStringParameters
    ): AnonymousSearchCriteriaDto
}
