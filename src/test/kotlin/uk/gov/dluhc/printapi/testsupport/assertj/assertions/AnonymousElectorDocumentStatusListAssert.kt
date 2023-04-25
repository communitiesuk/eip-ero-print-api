package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus

class AnonymousElectorDocumentStatusListAssert(actual: List<AnonymousElectorDocumentStatus>?) :
    AbstractAssert<AnonymousElectorDocumentStatusListAssert, List<AnonymousElectorDocumentStatus>?>(actual, AnonymousElectorDocumentStatusListAssert::class.java) {

    fun hasSize(expected: Int): AnonymousElectorDocumentStatusListAssert {
        isNotNull
        with(actual!!) {
            if (size != expected) {
                failWithMessage("Expected $expected statusHistory entries, but was $size")
            }
        }
        return this
    }

    fun hasStatus(expected: AnonymousElectorDocumentStatus.Status): AnonymousElectorDocumentStatusListAssert {
        isNotNull
        assertThat(actual).anyMatch {
            it.status == expected
        }
        return this
    }
}
