package uk.gov.dluhc.printapi.testsupport.assertj.assertions

import org.assertj.core.api.Assertions
import org.assertj.core.util.CheckReturnValue
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.dto.CertificateSearchSummaryResults
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults

/**
 * Entry point for assertions of different data types. Each method in this class is a static factory for the
 * type-specific assertion objects.
 */
object Assertions : Assertions() {
    /**
     * Creates a new instance of `[AddressAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: Address?): AddressAssert {
        return AddressAssert(actual)
    }

    /**
     * Creates a new instance of `[CertificateAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: Certificate?): CertificateAssert {
        return CertificateAssert(actual)
    }

    /**
     * Creates a new instance of `[TemporaryCertificateAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: TemporaryCertificate?): TemporaryCertificateAssert {
        return TemporaryCertificateAssert(actual)
    }

    /**
     * Creates a new instance of `[AnonymousElectorDocumentCertificateAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: AnonymousElectorDocument?): AnonymousElectorDocumentCertificateAssert {
        return AnonymousElectorDocumentCertificateAssert(actual)
    }

    /**
     * Creates a new instance of `[DeliveryAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: Delivery?): DeliveryAssert {
        return DeliveryAssert(actual)
    }

    /**
     * Creates a new instance of `[ElectoralRegistrationOfficeAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: ElectoralRegistrationOffice?): ElectoralRegistrationOfficeAssert {
        return ElectoralRegistrationOfficeAssert(actual)
    }

    /**
     * Creates a new instance of `[PrintRequestAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: PrintRequest?): PrintRequestAssert {
        return PrintRequestAssert(actual)
    }

    /**
     * Creates a new instance of `[PrintRequestStatusAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: PrintRequestStatus?): PrintRequestStatusAssert {
        return PrintRequestStatusAssert(actual)
    }

    /**
     * Creates a new instance of `[AnonymousElectorDocumentStatusListAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: List<AnonymousElectorDocumentStatus>?): AnonymousElectorDocumentStatusListAssert {
        return AnonymousElectorDocumentStatusListAssert(actual)
    }

    /**
     * Creates a new instance of `[AnonymousSearchSummaryResultsAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: AnonymousSearchSummaryResults?): AnonymousSearchSummaryResultsAssert {
        return AnonymousSearchSummaryResultsAssert(actual)
    }

    /**
     * Creates a new instance of `[CertificateSearchSummaryResultsAssert]`.
     *
     * @param actual the actual value.
     * @return the created assertion object.
     */
    @CheckReturnValue
    fun assertThat(actual: CertificateSearchSummaryResults?): CertificateSearchSummaryResultsAssert {
        return CertificateSearchSummaryResultsAssert(actual)
    }
}
