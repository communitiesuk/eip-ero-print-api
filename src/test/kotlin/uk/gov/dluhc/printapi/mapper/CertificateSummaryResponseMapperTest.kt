package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.mapper.CertificateSummaryResponseMapperImpl
import uk.gov.dluhc.printapi.database.mapper.PrintRequestStatusMapper
import uk.gov.dluhc.printapi.dto.StatusDto
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.models.PrintRequestStatus
import uk.gov.dluhc.printapi.models.PrintRequestSummary
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildPrintRequestSummaryDto
import java.time.Instant
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class CertificateSummaryResponseMapperTest {
    @InjectMocks
    private lateinit var mapper: CertificateSummaryResponseMapperImpl

    @Mock
    private lateinit var printRequestStatusMapper: PrintRequestStatusMapper

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Test
    fun `should map certificate summary dto to certificate summary response`() {
        // Given
        val request1 = buildPrintRequestSummaryDto(
            status = StatusDto.IN_PRODUCTION,
            eventDateTime = Instant.now().minusSeconds(100)
        )
        val request2 = buildPrintRequestSummaryDto(status = StatusDto.DISPATCHED, eventDateTime = Instant.now())
        val dto = buildCertificateSummaryDto(printRequests = listOf(request1, request2))
        given(printRequestStatusMapper.toPrintRequestStatus(any())).willReturn(
            PrintRequestStatus.PRINT_MINUS_PROCESSING,
            PrintRequestStatus.DISPATCHED
        )
        val dateTime1 = Instant.now().minusSeconds(100).atOffset(ZoneOffset.UTC)
        val dateTime2 = Instant.now().atOffset(ZoneOffset.UTC)
        given(instantMapper.toOffsetDateTime(any())).willReturn(dateTime1, dateTime2)
        val expectedRequestSummary1 = PrintRequestSummary(
            status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
            userId = request1.userId,
            dateTime = dateTime1,
            message = request1.message
        )
        val expectedRequestSummary2 = PrintRequestSummary(
            status = PrintRequestStatus.DISPATCHED,
            userId = request2.userId,
            dateTime = dateTime2,
            message = request2.message
        )
        val expected = with(dto) {
            CertificateSummaryResponse(
                vacNumber = vacNumber,
                printRequestSummaries = listOf(expectedRequestSummary1, expectedRequestSummary2)
            )
        }

        // When
        val actual = mapper.toCertificateSummaryResponse(dto)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
        verify(printRequestStatusMapper).toPrintRequestStatus(request1.status)
        verify(printRequestStatusMapper).toPrintRequestStatus(request2.status)
        verify(instantMapper).toOffsetDateTime(request1.dateTime)
        verify(instantMapper).toOffsetDateTime(request2.dateTime)
    }
}
