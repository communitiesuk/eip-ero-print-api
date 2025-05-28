package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep as StatusStepMessageEnum
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse.StatusStep as StatusStepModelEnum

class StatusStepMapperTest {
    val mapper = StatusStepMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "PROCESSED, PROCESSED",
            "IN_PRODUCTION, IN_MINUS_PRODUCTION",
            "PRINTED, PRINTED",
            "DISPATCHED, DISPATCHED",
            "NOT_DELIVERED, NOT_MINUS_DELIVERED"
        ]
    )
    fun `should map StatusStep model enum to StatusStep message enum`(statusStep: StatusStepModelEnum, expected: StatusStepMessageEnum) {
        // Given
        // When
        val actual = mapper.toStatusStepMessageEnum(statusStep)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
