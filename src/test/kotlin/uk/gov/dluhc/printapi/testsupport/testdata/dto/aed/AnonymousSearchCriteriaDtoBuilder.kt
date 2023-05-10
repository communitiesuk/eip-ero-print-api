package uk.gov.dluhc.printapi.testsupport.testdata.dto.aed

import uk.gov.dluhc.printapi.dto.aed.AedSearchBy
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId

fun buildAnonymousSearchCriteriaDto(
    eroId: String = aValidEroId(),
    page: Int = 1,
    pageSize: Int = 100,
    searchBy: AedSearchBy? = null,
    searchValue: String? = null,
): AnonymousSearchCriteriaDto {
    return AnonymousSearchCriteriaDto(
        eroId = eroId,
        page = page,
        pageSize = pageSize,
        searchBy = searchBy,
        searchValue = searchValue,
    )
}
