package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.util.CheckReturnValue
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import java.time.LocalDate
import java.util.Objects

/**
 * [TemporaryCertificate] specific assertions.
 */
class TemporaryCertificateAssert
/**
 * Creates a new `[TemporaryCertificateAssert]` to make assertions on actual TemporaryCertificate.
 * @param actual the TemporaryCertificate we want to make assertions on.
 */
(actual: TemporaryCertificate?) :
    AbstractObjectAssert<TemporaryCertificateAssert?, TemporaryCertificate?>(
        actual,
        TemporaryCertificateAssert::class.java
    ) {

    /**
     * Verifies that the actual TemporaryCertificate's finalRetentionRemovalDate is equal to the given one.
     * @param finalRetentionRemovalDate the given finalRetentionRemovalDate to compare the actual TemporaryCertificate's one.
     * @return this assertion object.
     * @throws AssertionError - if the actual TemporaryCertificate's finalRetentionRemovalDate is not equal to the given one.
     */
    fun hasFinalRetentionRemovalDate(finalRetentionRemovalDate: LocalDate?): TemporaryCertificateAssert {
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
         * With a static import, one can write directly: `assertThat(myTemporaryCertificate)` and get specific assertion
         * with code completion.
         * @param actual the TemporaryCertificate we want to make assertions on.
         * @return a new `[TemporaryCertificateAssert]`
         */
        @CheckReturnValue
        fun assertThat(actual: TemporaryCertificate?): TemporaryCertificateAssert {
            return TemporaryCertificateAssert(actual)
        }
    }
}
