package uk.gov.dluhc.printapi.validator.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildGenerateTemporaryCertificateDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GenerateTemporaryCertificateValidatorTest {

    companion object {
        private val TODAY = Instant.parse("2023-02-04T09:48:13.631Z")
        private val FIXED_CLOCK = Clock.fixed(TODAY, ZoneOffset.UTC)
        private const val MAX_ADVANCE_DAYS: Long = 10
    }

    private val validator = GenerateTemporaryCertificateValidator(FIXED_CLOCK, MAX_ADVANCE_DAYS)

    @Test
    fun `should validate successfully`() {
        // Given
        val validDate = LocalDate.parse("2023-02-04")
        val validDto = buildGenerateTemporaryCertificateDto(
            validOnDate = validDate
        )

        // When / Then
        assertDoesNotThrow { validator.validate(validDto) }
    }

    @Test
    fun `should validate with errors given validOnDate in the past`() {
        // Given
        val invalidDate = LocalDate.parse("2023-02-03")
        val inValidDto = buildGenerateTemporaryCertificateDto(
            validOnDate = invalidDate
        )

        // When
        val exception = catchThrowableOfType(GenerateTemporaryCertificateValidationException::class.java) {
            validator.validate(inValidDto)
        }

        // Then
        assertThat(exception)
            .hasMessage("Temporary Certificate validOnDate cannot be in the past")
    }

    @Test
    fun `should validate with errors given validOnDate too far in the future`() {
        // Given
        val invalidDate = LocalDate.parse("2023-02-15")
        val inValidDto = buildGenerateTemporaryCertificateDto(
            validOnDate = invalidDate
        )

        val exception = catchThrowableOfType(GenerateTemporaryCertificateValidationException::class.java) {
            validator.validate(inValidDto)
        }

        // Then
        assertThat(exception)
            .hasMessage("Temporary Certificate validOnDate cannot be greater than 10 calendar days in the future (cannot be after 2023-02-14)")
    }
}
