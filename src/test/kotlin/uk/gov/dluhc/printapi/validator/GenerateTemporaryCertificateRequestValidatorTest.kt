package uk.gov.dluhc.printapi.validator

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.validation.Errors
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateTemporaryCertificateRequest
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class GenerateTemporaryCertificateRequestValidatorTest {

    companion object {
        private val TODAY = Instant.parse("2023-02-04T09:48:13.631Z")
        private val FIXED_CLOCK = Clock.fixed(TODAY, ZoneOffset.UTC)
        private const val MAX_ADVANCE_DAYS: Long = 10
    }

    private val validator = GenerateTemporaryCertificateRequestValidator(FIXED_CLOCK, MAX_ADVANCE_DAYS)

    @Test
    fun `should validate successfully`() {
        // Given
        val errors = mock<Errors>()
        val validDate = LocalDate.parse("2023-02-04")
        val validRequest = buildGenerateTemporaryCertificateRequest(
            validOnDate = validDate
        )

        // When
        validator.validate(validRequest, errors)

        // Then
        verifyNoInteractions(errors)
    }

    @Test
    fun `should validate with errors given validOnDate in the past`() {
        // Given
        val errors = mock<Errors>()
        val invalidDate = LocalDate.parse("2023-02-03")
        val inValidRequest = buildGenerateTemporaryCertificateRequest(
            validOnDate = invalidDate
        )

        // When
        validator.validate(inValidRequest, errors)

        // Then
        verify(errors).rejectValue(
            "validOnDate",
            "generateTemporaryCertificateRequest.validOnDate.minimum",
            "date cannot be in the past"
        )
    }

    @Test
    fun `should validate with errors given validOnDate too far in the future`() {
        // Given
        val errors = mock<Errors>()
        val invalidDate = LocalDate.parse("2023-02-15")
        val inValidRequest = buildGenerateTemporaryCertificateRequest(
            validOnDate = invalidDate
        )

        // When
        validator.validate(inValidRequest, errors)

        // Then
        verify(errors).rejectValue(
            "validOnDate",
            "generateTemporaryCertificateRequest.validOnDate.maximum",
            "date cannot be greater than 10 in the future (cannot be after 2023-02-14)"
        )
    }
}
