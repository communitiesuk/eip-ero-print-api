package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

internal class CertificateNumberGeneratorTest {

    private val fixedClock = Clock.fixed(Instant.MIN, ZoneId.of("UTC"))
    private val certificateNumberGenerator = CertificateNumberGenerator(fixedClock)

    @Test
    fun `should generate certificate number`() {
        // Given

        // When
        val certificateNumber = certificateNumberGenerator.generateCertificateNumber()

        // Then
        assertThat(certificateNumber).containsPattern(Regex("^[0-9AC-HJ-NP-RT-Z]{20}$").pattern)
    }

    @Test
    fun `should generate unique certificate numbers`() {
        // Given
        val vacNumbers = mutableListOf<String>()

        // When
        repeat(100) {
            vacNumbers.add(certificateNumberGenerator.generateCertificateNumber())
        }

        // Then
        assertThat(vacNumbers).doesNotHaveDuplicates()
    }
}
