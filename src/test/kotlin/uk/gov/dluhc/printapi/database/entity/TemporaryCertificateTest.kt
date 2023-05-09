package uk.gov.dluhc.printapi.database.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificateStatus
import java.util.NoSuchElementException

internal class TemporaryCertificateTest {

    @Nested
    inner class GetStatus {
        @Test
        fun `should fail to get TemporaryCertificateStatus given certificate has no status history`() {
            val temporaryCertificate = buildTemporaryCertificate(statusHistory = mutableListOf())

            // When
            val error = catchException { temporaryCertificate.status }

            // Then
            assertThat(error).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `should get TemporaryCertificateStatus given certificate has one statuses`() {
            val newStatus = buildTemporaryCertificateStatus()
            val temporaryCertificate = buildTemporaryCertificate(statusHistory = mutableListOf(newStatus))

            // When
            val actual = temporaryCertificate.status

            // Then
            assertThat(actual).isEqualTo(newStatus.status)
        }
    }

    @Nested
    inner class AddTemporaryCertificateStatus {
        @Test
        fun `should add TemporaryCertificateStatus given certificate without existing statuses`() {
            val newStatus = buildTemporaryCertificateStatus()
            val temporaryCertificate = buildTemporaryCertificate(statusHistory = mutableListOf())

            // When
            val actual = temporaryCertificate.addTemporaryCertificateStatus(newStatus)

            // Then
            assertThat(actual.statusHistory).containsExactly(newStatus)
        }

        @Test
        fun `should add TemporaryCertificateStatus given certificate has existing statuses`() {
            val newStatus = buildTemporaryCertificateStatus()
            val existingStatus = buildTemporaryCertificateStatus()
            val temporaryCertificate = buildTemporaryCertificate(statusHistory = mutableListOf(existingStatus))

            // When
            val actual = temporaryCertificate.addTemporaryCertificateStatus(newStatus)

            // Then
            assertThat(actual.statusHistory).containsOnly(existingStatus, newStatus)
        }
    }

    @Nested
    inner class GetNameOnCertificate {
        @Test
        fun `should get NameOnCertificate given applicant has middle names`() {
            val firstName = "John"
            val middleNames = "Stewart"
            val surname = "Oliver"
            val temporaryCertificate = buildTemporaryCertificate(
                firstName = firstName,
                middleNames = middleNames,
                surname = surname
            )

            // When
            val actual = temporaryCertificate.getNameOnCertificate()

            // Then
            assertThat(actual).isEqualTo("John Stewart Oliver")
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = ["  "])
        fun `should get NameOnCertificate given applicant has no middle names`(middleNames: String?) {
            val temporaryCertificate =
                buildTemporaryCertificate(firstName = "John", middleNames = middleNames, surname = "Oliver")

            // When
            val actual = temporaryCertificate.getNameOnCertificate()

            // Then
            assertThat(actual).isEqualTo("John Oliver")
        }
    }
}
