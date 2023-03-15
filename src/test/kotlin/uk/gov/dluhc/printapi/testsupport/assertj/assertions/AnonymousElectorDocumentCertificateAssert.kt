package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.util.CheckReturnValue
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import java.time.LocalDate
import java.util.Objects

/**
 * [AnonymousElectorDocument] specific assertions.
 */
class AnonymousElectorDocumentCertificateAssert
/**
 * Creates a new `[AnonymousElectorDocumentCertificateAssert]` to make assertions on actual AnonymousElectorDocument.
 * @param actual the AnonymousElectorDocument we want to make assertions on.
 */
(actual: AnonymousElectorDocument?) :
    AbstractObjectAssert<AnonymousElectorDocumentCertificateAssert?, AnonymousElectorDocument?>(
        actual,
        AnonymousElectorDocumentCertificateAssert::class.java
    ) {

    /**
     * Verifies that the actual AnonymousElectorDocument's finalRetentionRemovalDate is equal to the given one.
     * @param finalRetentionRemovalDate the given finalRetentionRemovalDate to compare the actual AnonymousElectorDocument's one.
     * @return this assertion object.
     * @throws AssertionError - if the actual AnonymousElectorDocument's finalRetentionRemovalDate is not equal to the given one.
     */
    fun hasFinalRetentionRemovalDate(finalRetentionRemovalDate: LocalDate?): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting finalRetentionRemovalDate of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualFinalRetentionRemovalDate = actual!!.finalRetentionRemovalDate
        if (!Objects.deepEquals(actualFinalRetentionRemovalDate, finalRetentionRemovalDate)) {
            failWithMessage(assertjErrorMessage, actual, finalRetentionRemovalDate, actualFinalRetentionRemovalDate)
        }

        return this
    }

    companion object {
        /**
         * An entry point for CertificateAssert to follow AssertJ standard `assertThat()` statements.<br></br>
         * With a static import, one can write directly: `assertThat(myAnonymousElectorDocument)` and get specific assertion
         * with code completion.
         * @param actual the AnonymousElectorDocument we want to make assertions on.
         * @return a new `[AnonymousElectorDocumentCertificateAssert]`
         */
        @CheckReturnValue
        fun assertThat(actual: AnonymousElectorDocument?): AnonymousElectorDocumentCertificateAssert {
            return AnonymousElectorDocumentCertificateAssert(actual)
        }
    }
}
