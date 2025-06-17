package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
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
            "PROCESSED, FAILED, PRINT_PROVIDER_VALIDATION_FAILED",
            "IN_MINUS_PRODUCTION, SUCCESS, IN_PRODUCTION",
            "PRINTED, SUCCESS, PRINTED",
            "DISPATCHED, SUCCESS, DISPATCHED",
            "NOT_MINUS_DELIVERED, FAILED, NOT_DELIVERED",
        ]
    )
    fun `should map statusStep and status to entity status`(
        statusStep: ProcessPrintResponseMessage.StatusStep,
        status: ProcessPrintResponseMessage.Status,
        expected: Status,
    ) {
        // Given
        // When
        val actual = statusMapper.toStatusEntityEnum(statusStep, status)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "IN_MINUS_PRODUCTION, FAILED, Print status cannot be in statusStep [IN_MINUS_PRODUCTION] when the status is [FAILED]",
            "PRINTED, FAILED, Print status cannot be in statusStep [PRINTED] when the status is [FAILED]",
            "DISPATCHED, FAILED, Print status cannot be in statusStep [DISPATCHED] when the status is [FAILED]",
            "NOT_MINUS_DELIVERED, SUCCESS, Print status cannot be in statusStep [NOT_MINUS_DELIVERED] when the status is [SUCCESS]",
        ]
    )
    fun `should throw exception given invalid combination of statusStep and status`(
        statusStep: ProcessPrintResponseMessage.StatusStep,
        status: ProcessPrintResponseMessage.Status,
        message: String
    ) {
        // Given
        // When
        val ex = Assertions.catchThrowableOfType(IllegalArgumentException::class.java) {
            statusMapper.toStatusEntityEnum(statusStep, status)
        }

        // Then
        assertThat(ex).isNotNull.hasMessage(message)
    }
}
