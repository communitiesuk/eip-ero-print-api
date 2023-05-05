package uk.gov.dluhc.printapi.testsupport.testdata.dto.aed

import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults

fun buildAnonymousSearchSummaryResults(
    page: Int = 1,
    pageSize: Int = 100,
    totalPages: Int = 1,
    results: List<AnonymousSearchSummaryDto> = listOf(buildAnonymousSearchSummaryDto()),
    totalResults: Int = results.size,
): AnonymousSearchSummaryResults {
    return AnonymousSearchSummaryResults(
        page = page,
        pageSize = pageSize,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results,
    )
}
