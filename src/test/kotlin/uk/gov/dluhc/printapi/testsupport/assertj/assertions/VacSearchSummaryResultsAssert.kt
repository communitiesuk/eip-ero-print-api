package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import uk.gov.dluhc.printapi.dto.VacSearchSummaryResults
import uk.gov.dluhc.printapi.dto.VacSummaryDto

class VacSearchSummaryResultsAssert(actual: VacSearchSummaryResults?) :
    AbstractObjectAssert<VacSearchSummaryResultsAssert, VacSearchSummaryResults?>(
        actual,
        VacSearchSummaryResultsAssert::class.java
    ) {

    fun isPage(expected: Int): VacSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (page != expected) {
                failWithMessage("Expected to be page number $expected but was $page")
            }
        }
        return this
    }

    fun hasPageSize(expected: Int): VacSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (pageSize != expected) {
                failWithMessage("Expected page size to be $expected but was $pageSize")
            }
        }
        return this
    }

    fun hasTotalPages(expected: Int): VacSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (totalPages != expected) {
                failWithMessage("Expected total pages to be $expected but was $totalPages")
            }
        }
        return this
    }

    fun hasTotalResults(expected: Int): VacSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (totalResults != expected) {
                failWithMessage("Expected total results to be $expected but was $totalResults")
            }
        }
        return this
    }

    fun hasResults(expected: List<VacSummaryDto>): VacSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (results != expected) {
                failWithMessage("Expected results to be $expected but was $results")
            }
        }
        return this
    }

    fun resultsAreForApplicationReferences(vararg expected: String): VacSearchSummaryResultsAssert {
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
