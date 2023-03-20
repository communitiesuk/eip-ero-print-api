package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.Delivery
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
     * Verifies that the actual AnonymousElectorDocument's id is not null.
     * @return this assertion object.
     * @throws AssertionError - if the actual AnonymousElectorDocument's id is null.
     */
    fun hasId(): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting id of:\n  <%s>\nto be non-null"

        Assertions.assertThat(actual!!.id)
            .overridingErrorMessage(assertjErrorMessage, actual)
            .isNotNull

        // return the current assertion for method chaining
        return this
    }

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

    fun hasDelivery(expectedDelivery: Delivery): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        DeliveryAssert(actual!!.delivery)
            .hasId()
            .hasAddressee(expectedDelivery.addressee)
            .hasAddress(expectedDelivery.address)
            .hasAddressFormat(expectedDelivery.addressFormat)
            .hasDeliveryClass(expectedDelivery.deliveryClass)
            .hasDeliveryAddressType(expectedDelivery.deliveryAddressType)
            .hasDateCreated()
            .hasCreatedBy()
            .hasVersion()

        return this
    }

    fun hasDateCreated(): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        val assertjErrorMessage =
            "\nExpecting dateCreated of:\n  <%s>\nto be non-null"

        val actualDateCreated = actual!!.dateCreated
        Assertions.assertThat(actualDateCreated)
            .overridingErrorMessage(assertjErrorMessage, actual)
            .isNotNull

        return this
    }
}
