package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import jakarta.annotation.Generated
import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.internal.Iterables
import org.assertj.core.util.CheckReturnValue
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Objects
import java.util.UUID

/**
 * [PrintRequest] specific assertions - Generated by CustomAssertionGenerator.
 */
@Generated(value = ["assertj-assertions-generator"])
class PrintRequestAssert
/**
 * Creates a new `[PrintRequestAssert]` to make assertions on actual PrintRequest.
 * @param actual the PrintRequest we want to make assertions on.
 */
(actual: PrintRequest?) :
    AbstractObjectAssert<PrintRequestAssert?, PrintRequest?>(
        actual,
        PrintRequestAssert::class.java
    ) {
    /**
     * Verifies that the actual PrintRequest's batchId is equal to the given one.
     * @param batchId the given batchId to compare the actual PrintRequest's batchId to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's batchId is not equal to the given one.
     */
    fun hasBatchId(batchId: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting batchId of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualBatchId = actual!!.batchId
        if (!Objects.deepEquals(actualBatchId, batchId)) {
            failWithMessage(assertjErrorMessage, actual, batchId, actualBatchId)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's certificateLanguage is equal to the given one.
     * @param certificateLanguage the given certificateLanguage to compare the actual PrintRequest's certificateLanguage to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's certificateLanguage is not equal to the given one.
     */
    fun hasCertificateLanguage(certificateLanguage: CertificateLanguage?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting certificateLanguage of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualCertificateLanguage = actual!!.certificateLanguage
        if (!Objects.deepEquals(actualCertificateLanguage, certificateLanguage)) {
            failWithMessage(assertjErrorMessage, actual, certificateLanguage, actualCertificateLanguage)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's createdBy is not null.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's createdBy is null.
     */
    fun hasCreatedBy(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting createdBy of:\n  <%s>\nto be non-null"

        assertThat(actual!!.createdBy)
            .overridingErrorMessage(assertjErrorMessage, actual)
            .isNotNull

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's createdBy is equal to the given one.
     * @param createdBy the given createdBy to compare the actual PrintRequest's createdBy to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's createdBy is not equal to the given one.
     */
    fun hasCreatedBy(createdBy: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting createdBy of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualCreatedBy = actual!!.createdBy
        if (!Objects.deepEquals(actualCreatedBy, createdBy)) {
            failWithMessage(assertjErrorMessage, actual, createdBy, actualCreatedBy)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's currentStatus is equal to the given one.
     * @param currentStatus the given currentStatus to compare the actual PrintRequest's currentStatus to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's currentStatus is not equal to the given one.
     */
    fun hasCurrentStatus(currentStatus: PrintRequestStatus): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        PrintRequestStatusAssert(actual!!.getCurrentStatus())
            .hasId()
            .hasStatus(currentStatus.status)
            .hasMessage(currentStatus.message)
            .hasEventDateTime(currentStatus.eventDateTime!!)
            .hasDateCreated()
            .hasCreatedBy()
            .hasVersion()

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's dateCreated is not null.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's dateCreated is null.
     */
    fun hasDateCreated(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting dateCreated of:\n  <%s>\nto be non-null"

        assertThat(actual!!.dateCreated)
            .overridingErrorMessage(assertjErrorMessage, actual)
            .isNotNull

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's dateCreated is equal within margin to the given one.
     * @param dateCreated the given dateCreated to compare the actual PrintRequest's dateCreated to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's dateCreated is not equal to the given one.
     */
    fun hasDateCreated(dateCreated: Instant?, margin: Long = 5): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        val assertjErrorMessage =
            "\nExpecting dateCreated of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        val actualDateCreated = actual!!.dateCreated
        assertThat(actualDateCreated)
            .overridingErrorMessage(
                assertjErrorMessage,
                actual,
                actualDateCreated,
                dateCreated
            )
            .isCloseTo(dateCreated, within(margin, ChronoUnit.SECONDS))

        return this
    }

    /**
     * Verifies that the actual PrintRequest's delivery is equal to the given one.
     * @param delivery the given delivery to compare the actual PrintRequest's delivery to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's delivery is not equal to the given one.
     */
    fun hasDelivery(delivery: Delivery): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        DeliveryAssert(actual!!.delivery)
            .hasId()
            .hasAddressee(delivery.addressee)
            .hasAddress(delivery.address)
            .hasAddressFormat(delivery.addressFormat)
            .hasDeliveryClass(delivery.deliveryClass)
            .hasDeliveryAddressType(delivery.deliveryAddressType)
            .hasDateCreated()
            .hasCreatedBy()
            .hasVersion()

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's eroEnglish is equal to the given one.
     * @param eroEnglish the given eroEnglish to compare the actual PrintRequest's eroEnglish to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's eroEnglish is not equal to the given one.
     */
    fun hasEroEnglish(eroEnglish: ElectoralRegistrationOffice): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        ElectoralRegistrationOfficeAssert(actual!!.eroEnglish)
            .hasId()
            .hasName(eroEnglish.name)
            .hasPhoneNumber(eroEnglish.phoneNumber)
            .hasEmailAddress(eroEnglish.emailAddress)
            .hasWebsite(eroEnglish.website)
            .hasAddress(eroEnglish.address!!)
            .hasDateCreated()
            .hasCreatedBy()
            .hasVersion()

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's eroWelsh is equal to the given one.
     * @param eroWelsh the given eroWelsh to compare the actual PrintRequest's eroWelsh to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's eroWelsh is not equal to the given one.
     */
    fun hasEroWelsh(eroWelsh: ElectoralRegistrationOffice?): PrintRequestAssert {
        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting eroWelsh of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualEroWelsh = actual!!.eroWelsh
        if ((actualEroWelsh == null) xor (eroWelsh == null)) {
            failWithMessage(assertjErrorMessage, actual, eroWelsh, actualEroWelsh)
        }

        if (eroWelsh != null) {
            ElectoralRegistrationOfficeAssert(actualEroWelsh)
                .hasId()
                .hasName(eroWelsh.name)
                .hasPhoneNumber(eroWelsh.phoneNumber)
                .hasEmailAddress(eroWelsh.emailAddress)
                .hasWebsite(eroWelsh.website)
                .hasAddress(eroWelsh.address!!)
                .hasDateCreated()
                .hasCreatedBy()
                .hasVersion()
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's firstName is equal to the given one.
     * @param firstName the given firstName to compare the actual PrintRequest's firstName to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's firstName is not equal to the given one.
     */
    fun hasFirstName(firstName: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting firstName of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualFirstName = actual!!.firstName
        if (!Objects.deepEquals(actualFirstName, firstName)) {
            failWithMessage(assertjErrorMessage, actual, firstName, actualFirstName)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's id is not null.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's id is null.
     */
    fun hasId(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting id of:\n  <%s>\nto be non-null"

        assertThat(actual!!.id)
            .overridingErrorMessage(assertjErrorMessage, actual)
            .isNotNull

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's id is equal to the given one.
     * @param id the given id to compare the actual PrintRequest's id to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's id is not equal to the given one.
     */
    fun hasId(id: UUID?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting id of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualId = actual!!.id
        if (!Objects.deepEquals(actualId, id)) {
            failWithMessage(assertjErrorMessage, actual, id, actualId)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's middleNames is equal to the given one.
     * @param middleNames the given middleNames to compare the actual PrintRequest's middleNames to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's middleNames is not equal to the given one.
     */
    fun hasMiddleNames(middleNames: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting middleNames of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualMiddleNames = actual!!.middleNames
        if (!Objects.deepEquals(actualMiddleNames, middleNames)) {
            failWithMessage(assertjErrorMessage, actual, middleNames, actualMiddleNames)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's requestDateTime is equal to within margin of the given one.
     * @param requestDateTime the given requestDateTime to compare the actual PrintRequest's requestDateTime to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's requestDateTime is not equal to the given one.
     */
    fun hasRequestDateTime(requestDateTime: Instant?, margin: Long = 1): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting requestDateTime of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualRequestDateTime = actual!!.requestDateTime
        assertThat(actualRequestDateTime)
            .overridingErrorMessage(
                assertjErrorMessage,
                actual,
                actualRequestDateTime,
                requestDateTime
            )
            .isCloseTo(requestDateTime, within(margin, ChronoUnit.SECONDS))

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's requestId is equal to the given one.
     * @param requestId the given requestId to compare the actual PrintRequest's requestId to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's requestId is not equal to the given one.
     */
    fun hasRequestId(requestId: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting requestId of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualRequestId = actual!!.requestId
        if (!Objects.deepEquals(actualRequestId, requestId)) {
            failWithMessage(assertjErrorMessage, actual, requestId, actualRequestId)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's requestId is not null and has expected pattern.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's requestId is not present or does not match pattern.
     */
    fun hasRequestId(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting requestId of:\n  <%s>\nto contain pattern:\n  ^[a-f\\d]{24}\$\nbut was:\n  <%s>"

        // null safe check
        val actualRequestId = actual!!.requestId
        assertThat(actualRequestId)
            .overridingErrorMessage(assertjErrorMessage, actual, actualRequestId)
            .containsPattern(Regex("^[a-f\\d]{24}$").pattern)

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's statusHistory contains the given PrintRequestStatus elements.
     * @param statusHistory the given elements that should be contained in actual PrintRequest's statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory does not contain all given PrintRequestStatus elements.
     */
    fun hasStatusHistory(vararg statusHistory: PrintRequestStatus?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // check with standard error message, to set another message call: info.overridingErrorMessage("my error message");
        Iterables.instance().assertContains(info, actual!!.statusHistory, statusHistory)

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's statusHistory contains the given PrintRequestStatus elements in Collection.
     * @param statusHistory the given elements that should be contained in actual PrintRequest's statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory does not contain all given PrintRequestStatus elements.
     */
    fun hasStatusHistory(statusHistory: Collection<PrintRequestStatus>): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        for (requestStatus in statusHistory) {
            hasPrintRequestStatus(requestStatus.status!!)
                .hasId()
                .hasStatus(requestStatus.status)
                .hasEventDateTime(requestStatus.eventDateTime!!)
                .hasMessage(requestStatus.message)
                .hasDateCreated()
                .hasCreatedBy()
                .hasVersion()
        }

        // return the current assertion for method chaining
        return this
    }

    fun hasPrintRequestStatus(status: Status): PrintRequestStatusAssert {
        // check that actual Certificate we want to make assertions on is not null.
        isNotNull

        val printRequest = actual!!.statusHistory.find { it.status == status }

        // check that given PrintRequest collection is not null.
        if (printRequest == null) {
            failWithMessage("Expecting PrintRequestStatus to exist with status of `$status")
        }

        // return the print request assertion to continue assertions on the print request
        return PrintRequestStatusAssert(printRequest)
    }

    /**
     * Verifies that the actual PrintRequest's statusHistory contains **only** the given PrintRequestStatus elements and nothing else in whatever order.
     * @param statusHistory the given elements that should be contained in actual PrintRequest's statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory does not contain all given PrintRequestStatus elements.
     */
    fun hasOnlyStatusHistory(vararg statusHistory: PrintRequestStatus?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // check with standard error message, to set another message call: info.overridingErrorMessage("my error message");
        Iterables.instance().assertContainsOnly(info, actual!!.statusHistory, statusHistory)

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's statusHistory contains **only** the given PrintRequestStatus elements in Collection and nothing else in whatever order.
     * @param statusHistory the given elements that should be contained in actual PrintRequest's statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory does not contain all given PrintRequestStatus elements.
     */
    fun hasOnlyStatusHistory(statusHistory: Collection<PrintRequestStatus?>?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // check that given PrintRequestStatus collection is not null.
        if (statusHistory == null) {
            failWithMessage("Expecting statusHistory parameter not to be null.")
            return this // to fool Eclipse "Null pointer access" warning on toArray.
        }

        // check with standard error message, to set another message call: info.overridingErrorMessage("my error message");
        Iterables.instance().assertContainsOnly(info, actual!!.statusHistory, statusHistory.toTypedArray())

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's statusHistory does not contain the given PrintRequestStatus elements.
     *
     * @param statusHistory the given elements that should not be in actual PrintRequest's statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory contains any given PrintRequestStatus elements.
     */
    fun doesNotHaveStatusHistory(vararg statusHistory: PrintRequestStatus?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // check with standard error message (use overridingErrorMessage before contains to set your own message).
        Iterables.instance().assertDoesNotContain(info, actual!!.statusHistory, statusHistory)

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's statusHistory does not contain the given PrintRequestStatus elements in Collection.
     *
     * @param statusHistory the given elements that should not be in actual PrintRequest's statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory contains any given PrintRequestStatus elements.
     */
    fun doesNotHaveStatusHistory(statusHistory: Collection<PrintRequestStatus?>?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // check that given PrintRequestStatus collection is not null.
        if (statusHistory == null) {
            failWithMessage("Expecting statusHistory parameter not to be null.")
            return this // to fool Eclipse "Null pointer access" warning on toArray.
        }

        // check with standard error message (use overridingErrorMessage before contains to set your own message).
        Iterables.instance().assertDoesNotContain(info, actual!!.statusHistory, statusHistory.toTypedArray())

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest has no statusHistory.
     * @return this assertion object.
     * @throws AssertionError if the actual PrintRequest's statusHistory is not empty.
     */
    fun hasNoStatusHistory(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // we override the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting :\n  <%s>\nnot to have statusHistory but had :\n  <%s>"

        // check
        if (actual!!.statusHistory.iterator().hasNext()) {
            failWithMessage(assertjErrorMessage, actual, actual!!.statusHistory)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the data which needs to be removed after the initial retention period is null.
     * @return this assertion object.
     * @throws AssertionError if the data is not null.
     */
    fun doesNotHaveInitialRetentionPeriodData(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // we override the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting :\n  <%s>\nnot to have initial retention period data"

        // check
        if (actual!!.delivery != null || actual!!.supportingInformationFormat != null) {
            failWithMessage(assertjErrorMessage, actual)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the data which needs to be removed after the initial retention period still exists.
     * @return this assertion object.
     * @throws AssertionError if the data is null.
     */
    fun hasInitialRetentionPeriodData(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // we override the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting :\n  <%s>\n still to have initial retention period data"

        // check
        if (actual!!.delivery == null || actual!!.supportingInformationFormat == null) {
            failWithMessage(assertjErrorMessage, actual)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's supportingInformationFormat is equal to the given one.
     * @param supportingInformationFormat the given supportingInformationFormat to compare the actual PrintRequest's supportingInformationFormat to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's supportingInformationFormat is not equal to the given one.
     */
    fun hasSupportingInformationFormat(supportingInformationFormat: SupportingInformationFormat?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage =
            "\nExpecting supportingInformationFormat of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualSupportingInformationFormat = actual!!.supportingInformationFormat
        if (!Objects.deepEquals(actualSupportingInformationFormat, supportingInformationFormat)) {
            failWithMessage(assertjErrorMessage, actual, supportingInformationFormat, actualSupportingInformationFormat)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's surname is equal to the given one.
     * @param surname the given surname to compare the actual PrintRequest's surname to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's surname is not equal to the given one.
     */
    fun hasSurname(surname: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting surname of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualSurname = actual!!.surname
        if (!Objects.deepEquals(actualSurname, surname)) {
            failWithMessage(assertjErrorMessage, actual, surname, actualSurname)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's userId is equal to the given one.
     * @param userId the given userId to compare the actual PrintRequest's userId to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's userId is not equal to the given one.
     */
    fun hasUserId(userId: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting userId of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualUserId = actual!!.userId
        if (!Objects.deepEquals(actualUserId, userId)) {
            failWithMessage(assertjErrorMessage, actual, userId, actualUserId)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's vacVersion is equal to the given one.
     * @param vacVersion the given vacVersion to compare the actual PrintRequest's vacVersion to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's vacVersion is not equal to the given one.
     */
    fun hasVacVersion(vacVersion: String?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting vacVersion of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualVacVersion = actual!!.vacVersion
        if (!Objects.deepEquals(actualVacVersion, vacVersion)) {
            failWithMessage(assertjErrorMessage, actual, vacVersion, actualVacVersion)
        }

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's version is not null.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's version is null.
     */
    fun hasVersion(): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting version of:\n  <%s>\nto be non-null"

        assertThat(actual!!.version)
            .overridingErrorMessage(assertjErrorMessage, actual)
            .isNotNull

        // return the current assertion for method chaining
        return this
    }

    /**
     * Verifies that the actual PrintRequest's version is equal to the given one.
     * @param version the given version to compare the actual PrintRequest's version to.
     * @return this assertion object.
     * @throws AssertionError - if the actual PrintRequest's version is not equal to the given one.
     */
    fun hasVersion(version: Long?): PrintRequestAssert {
        // check that actual PrintRequest we want to make assertions on is not null.
        isNotNull

        // overrides the default error message with a more explicit one
        val assertjErrorMessage = "\nExpecting version of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"

        // null safe check
        val actualVersion = actual!!.version
        if (!Objects.deepEquals(actualVersion, version)) {
            failWithMessage(assertjErrorMessage, actual, version, actualVersion)
        }

        // return the current assertion for method chaining
        return this
    }

    companion object {
        /**
         * An entry point for PrintRequestAssert to follow AssertJ standard `assertThat()` statements.<br></br>
         * With a static import, one can write directly: `assertThat(myPrintRequest)` and get specific assertion with code completion.
         * @param actual the PrintRequest we want to make assertions on.
         * @return a new `[PrintRequestAssert]`
         */
        @CheckReturnValue
        fun assertThat(actual: PrintRequest?): PrintRequestAssert {
            return PrintRequestAssert(actual)
        }
    }
}
