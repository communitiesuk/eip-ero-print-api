package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.models.PrintRequestStatus

class PrintRequestStatusMapperTest {
    private val mapper = PrintRequestStatusMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "PENDING_ASSIGNMENT_TO_BATCH, PRINT_PROCESSING",
            "ASSIGNED_TO_BATCH, PRINT_PROCESSING",
            "SENT_TO_PRINT_PROVIDER, PRINT_PROCESSING",
            "RECEIVED_BY_PRINT_PROVIDER, PRINT_PROCESSING",
            "VALIDATED_BY_PRINT_PROVIDER, PRINT_PROCESSING",
            "IN_PRODUCTION, PRINT_PROCESSING",
            "PRINTED, PRINT_PROCESSING",
            "DISPATCHED, DISPATCHED",
            "NOT_DELIVERED, NOT_DELIVERED",
            "PRINT_PROVIDER_VALIDATION_FAILED, PRINT_FAILED",
        ]
    )
    fun `should map status dto enum to print request status`(status: PrintRequestStatusDto, expected: PrintRequestStatus) {
        // Given
        // When
        val actual = mapper.toPrintRequestStatus(status)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
