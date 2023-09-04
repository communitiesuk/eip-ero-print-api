package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.CertificateSearchSummaryResults
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto

fun buildCertificateSearchSummaryResults(
    page: Int = 1,
    pageSize: Int = 100,
    totalPages: Int = 1,
    results: List<CertificateSummaryDto> = listOf(buildCertificateSummaryDto()),
    totalResults: Int = results.size,
): CertificateSearchSummaryResults {
    return CertificateSearchSummaryResults(
        page = page,
        pageSize = pageSize,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results,
    )
}
