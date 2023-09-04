package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.CertificateSearchBy
import uk.gov.dluhc.printapi.dto.CertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId

fun buildCertificateSearchCriteriaDto(
    eroId: String = aValidEroId(),
    page: Int = 1,
    pageSize: Int = 100,
    searchBy: CertificateSearchBy? = null,
    searchValue: String? = null,
): CertificateSearchCriteriaDto {
    return CertificateSearchCriteriaDto(
        eroId = eroId,
        page = page,
        pageSize = pageSize,
        searchBy = searchBy,
        searchValue = searchValue,
    )
}
