package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.Status as StatusMessageEnum
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep as StatusStepMessageEnum
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse.Status as StatusModelEnum
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse.StatusStep as StatusStepModelEnum

@ExtendWith(MockitoExtension::class)
class ProcessPrintResponseMessageMapperTest {
    @InjectMocks
    private lateinit var mapper: ProcessPrintResponseMessageMapperImpl

    @Mock
    private lateinit var statusStepMapper: StatusStepMapper

    @ParameterizedTest
    @CsvSource(
        value = [
            "PROCESSED, SUCCESS, PROCESSED, SUCCESS",
            "IN_PRODUCTION, SUCCESS, IN_MINUS_PRODUCTION, SUCCESS",
            "PRINTED, SUCCESS, PRINTED, SUCCESS",
            "DISPATCHED, SUCCESS, DISPATCHED, SUCCESS",
            "PROCESSED, FAILED, PROCESSED, FAILED",
            "NOT_DELIVERED, FAILED, NOT_MINUS_DELIVERED, FAILED"
        ]
    )
    fun `should map PrintResponse to ProcessPrintResponseMessage`(
        statusStep: StatusStepModelEnum,
        status: StatusModelEnum,
        expectedStatusStep: StatusStepMessageEnum,
        expectedStatus: StatusMessageEnum
    ) {
        // Given
        val printResponse = buildPrintResponse(statusStep = statusStep, status = status)
        given(statusStepMapper.toStatusStepMessageEnum(any())).willReturn(expectedStatusStep)
        val expected = with(printResponse) {
            ProcessPrintResponseMessage(
                requestId = requestId,
                timestamp = timestamp,
                statusStep = expectedStatusStep,
                status = expectedStatus,
                message = message
            )
        }

        // When
        val actual = mapper.toProcessPrintResponseMessage(printResponse)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(statusStepMapper).toStatusStepMessageEnum(statusStep)
    }
}
