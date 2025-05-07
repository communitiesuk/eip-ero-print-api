package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import uk.gov.dluhc.printapi.dto.CertificateSearchSummaryResults
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto

class CertificateSearchSummaryResultsAssert(actual: CertificateSearchSummaryResults?) :
    AbstractObjectAssert<CertificateSearchSummaryResultsAssert, CertificateSearchSummaryResults?>(
        actual,
        CertificateSearchSummaryResultsAssert::class.java
    ) {

    fun isPage(expected: Int): CertificateSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (page != expected) {
                failWithMessage("Expected to be page number $expected but was $page")
            }
        }
        return this
    }

    fun hasPageSize(expected: Int): CertificateSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (pageSize != expected) {
                failWithMessage("Expected page size to be $expected but was $pageSize")
            }
        }
        return this
    }

    fun hasTotalPages(expected: Int): CertificateSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (totalPages != expected) {
                failWithMessage("Expected total pages to be $expected but was $totalPages")
            }
        }
        return this
    }

    fun hasTotalResults(expected: Int): CertificateSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (totalResults != expected) {
                failWithMessage("Expected total results to be $expected but was $totalResults")
            }
        }
        return this
    }

    fun hasResults(expected: List<CertificateSummaryDto>): CertificateSearchSummaryResultsAssert {
        isNotNull
        with(actual!!) {
            if (results != expected) {
                failWithMessage("Expected results to be $expected but was $results")
            }
        }
        return this
    }

    fun resultsAreForApplicationReferences(vararg expected: String): CertificateSearchSummaryResultsAssert {
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
