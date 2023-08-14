package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.models.PrintRequestStatus
import uk.gov.dluhc.printapi.models.VacPrintRequestSummary
import uk.gov.dluhc.printapi.models.VacSearchSummaryResponse
import uk.gov.dluhc.printapi.models.VacSummaryResponse
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacPrintRequestSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSearchSummaryResults
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSummaryDto
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class VacSearchSummaryResponseMapperTest {

    @Mock
    private lateinit var instantMapper: InstantMapper

    @Mock
    private lateinit var printRequestStatusMapper: PrintRequestStatusMapper

    @InjectMocks
    private lateinit var mapper: VacSummarySearchResponseMapperImpl

    @Test
    fun `should map VacSummaryDto to an VacSummaryResponse`() {
        // Given
        val expectedOffsetDateTime = OffsetDateTime.now()
        given(instantMapper.toOffsetDateTime(any())).willReturn(expectedOffsetDateTime)

        val expectedStatus = PrintRequestStatus.PRINT_MINUS_PROCESSING
        given(printRequestStatusMapper.toPrintRequestStatus(PrintRequestStatusDto.ASSIGNED_TO_BATCH)).willReturn(
            expectedStatus
        )

        val printRequest = buildVacPrintRequestSummaryDto(status = PrintRequestStatusDto.ASSIGNED_TO_BATCH)
        val vacSummaryDto = buildVacSummaryDto(printRequests = listOf(printRequest))
        val expected = with(vacSummaryDto) {
            VacSummaryResponse(
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                vacNumber = vacNumber,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                printRequestSummaries = listOf(
                    VacPrintRequestSummary(
                        status = expectedStatus,
                        dateTime = expectedOffsetDateTime,
                        userId = printRequest.userId,
                    )
                ),
            )
        }

        // When
        val actual = mapper.toVacSummaryResponse(vacSummaryDto)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map VacSearchSummaryResults dto to an VacSearchSummaryResponse Api`() {
        // Given
        val expectedOffsetDateTime = OffsetDateTime.now()
        given(instantMapper.toOffsetDateTime(any())).willReturn(expectedOffsetDateTime)

        val expectedStatus = PrintRequestStatus.PRINT_MINUS_PROCESSING
        given(printRequestStatusMapper.toPrintRequestStatus(any())).willReturn(expectedStatus)

        val printRequestDto = buildVacPrintRequestSummaryDto()
        val vacSummaryDto = buildVacSummaryDto(printRequests = listOf(printRequestDto))
        val results = buildVacSearchSummaryResults(
            page = 1,
            pageSize = 50,
            totalPages = 1,
            totalResults = 1,
            results = listOf(vacSummaryDto),
        )

        val expected = with(results) {
            VacSearchSummaryResponse(
                page = page,
                pageSize = pageSize,
                totalResults = totalResults,
                totalPages = totalPages,
                results = listOf(
                    VacSummaryResponse(
                        vacNumber = vacSummaryDto.vacNumber,
                        applicationReference = vacSummaryDto.applicationReference,
                        sourceReference = vacSummaryDto.sourceReference,
                        firstName = vacSummaryDto.firstName,
                        middleNames = vacSummaryDto.middleNames,
                        surname = vacSummaryDto.surname,
                        printRequestSummaries = listOf(
                            VacPrintRequestSummary(
                                userId = printRequestDto.userId,
                                dateTime = expectedOffsetDateTime,
                                status = expectedStatus
                            )
                        )
                    )
                )
            )
        }

        // When
        val actual = mapper.toVacSearchSummaryResponse(results)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
