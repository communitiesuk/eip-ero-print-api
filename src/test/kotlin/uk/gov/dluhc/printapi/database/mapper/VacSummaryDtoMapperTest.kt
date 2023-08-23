package uk.gov.dluhc.printapi.database.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.junit.jupiter.MockitoExtension
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacPrintRequestSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class VacSummaryDtoMapperTest {

    @InjectMocks
    private lateinit var mapper: VacSummaryDtoMapper

    @Test
    fun `should map Certificate to a VacSummaryDto`() {
        // Given
        val printRequestStatus = buildPrintRequestStatus(status = PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH)
        val printRequest = buildPrintRequest(printRequestStatuses = listOf(printRequestStatus))
        val entity = buildCertificate(printRequests = listOf(printRequest))
        val expected = with(entity) {
            buildVacSummaryDto(
                sourceReference = sourceReference!!,
                applicationReference = applicationReference!!,
                vacNumber = vacNumber!!,
                firstName = printRequest.firstName!!,
                middleNames = printRequest.middleNames,
                surname = printRequest.surname!!,
                printRequests = listOf(
                    buildVacPrintRequestSummaryDto(
                        status = PrintRequestStatusDto.PENDING_ASSIGNMENT_TO_BATCH,
                        dateTime = printRequest.requestDateTime!!,
                        userId = printRequest.userId!!,
                    )
                ),
            )
        }

        // When
        val actual = mapper.certificateToVacSummaryDto(entity)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should use most recent print request status`() {
        // Given

        val printRequest = buildPrintRequest(
            printRequestStatuses = listOf(
                buildPrintRequestStatus(
                    status = PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH,
                    eventDateTime = Instant.now().minusSeconds(100)
                ),
                buildPrintRequestStatus(
                    status = PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
                    eventDateTime = Instant.now().minusSeconds(50)
                ),
                buildPrintRequestStatus(
                    status = PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER,
                    eventDateTime = Instant.now().minusSeconds(10)
                ),
            )
        )
        val entity = buildCertificate(printRequests = listOf(printRequest))
        val expected = with(entity) {
            buildVacSummaryDto(
                sourceReference = sourceReference!!,
                applicationReference = applicationReference!!,
                vacNumber = vacNumber!!,
                firstName = printRequest.firstName!!,
                middleNames = printRequest.middleNames,
                surname = printRequest.surname!!,
                printRequests = listOf(
                    buildVacPrintRequestSummaryDto(
                        status = PrintRequestStatusDto.SENT_TO_PRINT_PROVIDER,
                        dateTime = printRequest.requestDateTime!!,
                        userId = printRequest.userId!!,
                    )
                ),
            )
        }

        // When
        val actual = mapper.certificateToVacSummaryDto(entity)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
