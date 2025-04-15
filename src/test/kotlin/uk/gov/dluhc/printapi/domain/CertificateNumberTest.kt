package uk.gov.dluhc.printapi.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Test

internal class CertificateNumberTest {

    @Test
    fun `should create unique Certificate Number`() {
        // Given
        val certificateNumbers = mutableListOf<String>()

        // When
        repeat(100) { certificateNumbers.add(CertificateNumber.create()) }

        // Then
        assertThat(certificateNumbers).doesNotHaveDuplicates()
            .allSatisfy { assertThat(it).containsPattern(Regex("^[0-9AC-HJ-NP-RT-Z]{20}$").pattern) }
    }

    @Test
    fun `should create Certificate Number with lowest possible value`() {
        // Given
        val timestamp = 0
        val randomValue1 = 0
        val randomValue2: Short = 0
        val counter = 0

        // When
        val certificateNumber = CertificateNumber(timestamp, randomValue1, randomValue2, counter)

        // Then
        assertThat(certificateNumber.toString()).isEqualTo("00000000000000000000")
    }

    @Test
    fun `should create Certificate Number with highest possible value`() {
        // Given
        val timestamp = 2147483647
        val randomValue1 = 16777215
        val randomValue2: Short = 32767
        val counter = 16777215

        // When
        val certificateNumber = CertificateNumber(timestamp, randomValue1, randomValue2, counter)

        // Then
        assertThat(certificateNumber.toString()).isEqualTo("GZZZZZZZZZZQZZZZZZZ1")
    }

    @Test
    fun `should not create Certificate Number given value is not 20 characters in length`() {
        // Given
        val proposedCertificateNumber = "123XYZ"

        // When
        val ex = catchThrowableOfType(IllegalArgumentException::class.java) {
            CertificateNumber(proposedCertificateNumber)
        }

        // Then
        assertThat(ex.message).isEqualTo("CertificateNumber value length must be 20")
    }

    @Test
    fun `should not create Certificate Number given value contains characters not in the character set`() {
        // Given
        val proposedCertificateNumber = "gZZZZZZZZZZZxZZZZZZ7"

        // When
        val ex = catchThrowableOfType(IllegalArgumentException::class.java) {
            CertificateNumber(proposedCertificateNumber)
        }

        // Then
        assertThat(ex.message).isEqualTo("CertificateNumber value must only contain characters from [0123456789ACDEFGHJKLMNPQRTUVWXYZ]")
    }

    @Test
    fun `should not create Certificate Number given values last character is not a 0 or 1`() {
        // Given
        val proposedCertificateNumber = "1ZZZZZZZZZZZ3ZZZZZZ7"

        // When
        val ex = catchThrowableOfType(IllegalArgumentException::class.java) {
            CertificateNumber(proposedCertificateNumber)
        }

        // Then
        assertThat(ex.message).isEqualTo("CertificateNumber value last character must be a 0 or 1")
    }

    @Test
    fun `should create Certificate Number from other value`() {
        // Given
        val firstCertificateNumber = CertificateNumber()

        // When
        val certificateNumber = CertificateNumber(firstCertificateNumber.toString())

        // Then
        assertThat(certificateNumber).isEqualTo(firstCertificateNumber)
    }
}
