package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.mapper.PrintRequestStatusMapperImpl
import uk.gov.dluhc.printapi.dto.StatusDto
import uk.gov.dluhc.printapi.models.PrintRequestStatus

class PrintRequestStatusMapperTest {
    private val mapper = PrintRequestStatusMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "PENDING_ASSIGNMENT_TO_BATCH, PRINT_MINUS_PROCESSING",
            "ASSIGNED_TO_BATCH, PRINT_MINUS_PROCESSING",
            "SENT_TO_PRINT_PROVIDER, PRINT_MINUS_PROCESSING",
            "RECEIVED_BY_PRINT_PROVIDER, PRINT_MINUS_PROCESSING",
            "VALIDATED_BY_PRINT_PROVIDER, PRINT_MINUS_PROCESSING",
            "IN_PRODUCTION, PRINT_MINUS_PROCESSING",
            "DISPATCHED, DISPATCHED",
            "NOT_DELIVERED, NOT_MINUS_DELIVERED",
            "PRINT_PROVIDER_VALIDATION_FAILED, PRINT_MINUS_FAILED",
            "PRINT_PROVIDER_PRODUCTION_FAILED, PRINT_MINUS_FAILED",
            "PRINT_PROVIDER_DISPATCH_FAILED, PRINT_MINUS_FAILED"
        ]
    )
    fun `should map status dto enum to print request status`(status: StatusDto, expected: PrintRequestStatus) {
        // Given
        // When
        val actual = mapper.toPrintRequestStatus(status)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
