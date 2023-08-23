package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.VacSearchBy
import uk.gov.dluhc.printapi.dto.VacSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId

fun buildVacSearchCriteriaDto(
    eroId: String = aValidEroId(),
    page: Int = 1,
    pageSize: Int = 100,
    searchBy: VacSearchBy? = null,
    searchValue: String? = null,
): VacSearchCriteriaDto {
    return VacSearchCriteriaDto(
        eroId = eroId,
        page = page,
        pageSize = pageSize,
        searchBy = searchBy,
        searchValue = searchValue,
    )
}
