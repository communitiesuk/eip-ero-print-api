package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage

class StatusMapperTest {
    private val statusMapper: StatusMapper = StatusMapper()

    @ParameterizedTest
    @CsvSource(
        value = [
            "PROCESSED, SUCCESS, VALIDATED_BY_PRINT_PROVIDER",
            "IN_MINUS_PRODUCTION, SUCCESS, IN_PRODUCTION",
            "DISPATCHED, SUCCESS, DISPATCHED",
            "NOT_MINUS_DELIVERED, FAILED, NOT_DELIVERED",
            "PROCESSED, FAILED, PRINT_PROVIDER_VALIDATION_FAILED",
            "IN_MINUS_PRODUCTION, FAILED, PRINT_PROVIDER_PRODUCTION_FAILED",
            "DISPATCHED, FAILED, PRINT_PROVIDER_DISPATCH_FAILED"
        ]
    )
    fun `should map statusStep and status to entity status`(
        statusStep: ProcessPrintResponseMessage.StatusStep,
        status: ProcessPrintResponseMessage.Status,
        expected: Status
    ) {
        // Given
        // When
        val actual = statusMapper.toStatusEntityEnum(statusStep, status)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should throw exception given statusStep is NOT_MINUS_DELIVERED and status is SUCCESS`() {
        // Given
        // When
        val ex = Assertions.catchThrowableOfType(
            {
                statusMapper.toStatusEntityEnum(
                    ProcessPrintResponseMessage.StatusStep.NOT_MINUS_DELIVERED,
                    ProcessPrintResponseMessage.Status.SUCCESS
                )
            },
            IllegalArgumentException::class.java
        )

        // Then
        assertThat(ex).isNotNull.hasMessage("Print status cannot be in statusStep [NOT_MINUS_DELIVERED] when the status is [SUCCESS]")
    }
}
