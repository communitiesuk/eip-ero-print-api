package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import java.time.LocalDate
import java.util.Objects
import java.util.function.Consumer

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

    fun hasSupportingInformationFormat(expected: SupportingInformationFormat): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        val assertjErrorMessage = "\nExpecting supportingInformationFormat of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        val actualSupportingInformationFormat = actual!!.supportingInformationFormat
        if (!Objects.deepEquals(actualSupportingInformationFormat, expected)) {
            failWithMessage(assertjErrorMessage, actual, actualSupportingInformationFormat, expected)
        }

        return this
    }

    /**
     * Verifies that the actual AnonymousElectorDocument's initialRetentionRemovalDate is equal to the given one.
     * @param initialRetentionRemovalDate the given initialRetentionRemovalDate to compare the actual AnonymousElectorDocument's one.
     * @return this assertion object.
     * @throws AssertionError - if the actual AnonymousElectorDocument's initialRetentionRemovalDate is not equal to the given one.
     */
    fun hasInitialRetentionRemovalDate(initialRetentionRemovalDate: LocalDate?): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting initialRetentionRemovalDate of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualInitialRetentionRemovalDate = actual!!.initialRetentionRemovalDate
        if (!Objects.deepEquals(actualInitialRetentionRemovalDate, initialRetentionRemovalDate)) {
            failWithMessage(assertjErrorMessage, actual, initialRetentionRemovalDate, actualInitialRetentionRemovalDate)
        }

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

    /**
     * Verifies that the data which needs to be removed after the initial retention period is null.
     * @return this assertion object.
     * @throws AssertionError if the data is not null.
     */
    fun initialRetentionPeriodDataIsRemoved(): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        if (this.actual?.initialRetentionDataRemoved == false) {
            failWithMessage("Expecting initialRetentionDataRemoved to be true.")
        }

        if (this.actual?.supportingInformationFormat != null ||
            this.actual?.contactDetails?.address != null ||
            this.actual?.contactDetails?.email != null ||
            this.actual?.contactDetails?.phoneNumber != null ||
            this.actual?.delivery != null
        ) {
            failWithMessage("Expecting initial retention data to be removed.")
        }

        return this
    }

    /**
     * Verifies that the data which needs to be removed after the initial retention period still exists.
     * @return this assertion object.
     * @throws AssertionError if the data is null.
     */
    fun hasInitialRetentionPeriodData(): AnonymousElectorDocumentCertificateAssert {
        isNotNull

        if (this.actual?.initialRetentionDataRemoved == true) {
            failWithMessage("Expecting initialRetentionDataRemoved to be false.")
        }

        if (this.actual?.supportingInformationFormat == null ||
            this.actual?.contactDetails?.address == null
        ) {
            failWithMessage("Expecting initial retention data not to be removed (null).")
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

    /**
     * Allows for assertion chaining into the child [AnonymousElectorDocumentStatus] entities. Takes a lambda as the method argument
     * to call assertion methods provided by [AnonymousElectorDocumentStatusListAssert].
     * Returns this [AnonymousElectorDocumentCertificateAssert] to allow further chained assertions on the parent [AnonymousElectorDocument]
     */
    fun statusHistory(consumer: Consumer<AnonymousElectorDocumentStatusListAssert>): AnonymousElectorDocumentCertificateAssert {
        isNotNull
        with(actual!!) {
            consumer.accept(assertThat(statusHistory))
        }
        return this
    }
}
