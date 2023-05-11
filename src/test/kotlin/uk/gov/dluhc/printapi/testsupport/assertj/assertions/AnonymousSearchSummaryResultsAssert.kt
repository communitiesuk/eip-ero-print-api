package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults

class AnonymousSearchSummaryResultsAssert(actual: AnonymousSearchSummaryResults?) :
    AbstractObjectAssert<AnonymousSearchSummaryResultsAssert, AnonymousSearchSummaryResults?>(
        actual,
        AnonymousSearchSummaryResultsAssert::class.java
    ) {

    fun isPage(expected: Int): AnonymousSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (page != expected) {
                failWithMessage("Expected to be page number $expected but was $page")
            }
        }
        return this
    }

    fun hasPageSize(expected: Int): AnonymousSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (pageSize != expected) {
                failWithMessage("Expected page size to be $expected but was $pageSize")
            }
        }
        return this
    }

    fun hasTotalPages(expected: Int): AnonymousSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (totalPages != expected) {
                failWithMessage("Expected total pages to be $expected but was $totalPages")
            }
        }
        return this
    }

    fun hasTotalResults(expected: Int): AnonymousSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (totalResults != expected) {
                failWithMessage("Expected total results to be $expected but was $totalResults")
            }
        }
        return this
    }

    fun hasResults(expected: List<AnonymousSearchSummaryDto>): AnonymousSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (results != expected) {
                failWithMessage("Expected results to be $expected but was $results")
            }
        }
        return this
    }

    fun resultsAreForApplicationReferences(vararg expected: String): AnonymousSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            val applicationReferences = results.map { it.applicationReference }
            assertThat(applicationReferences)
                .containsExactly(*expected)
                .withFailMessage("Expected results to be for application references $expected but was $applicationReferences")
        }
        return this
    }
}
