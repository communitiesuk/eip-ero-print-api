package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.CertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.rest.CertificateSearchQueryStringParameters

@Mapper
interface CertificateSearchQueryStringParametersMapper {

    fun toCertificateSearchCriteriaDto(
        eroId: String,
        searchQueryParameters: CertificateSearchQueryStringParameters
    ): CertificateSearchCriteriaDto
}
