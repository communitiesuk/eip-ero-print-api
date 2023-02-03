package uk.gov.dluhc.printapi.database.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.VALIDATED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.dto.PrintRequestSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.aDifferentValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Instant
import java.time.Instant.now

internal class CertificateSummaryDtoMapperTest {

    private val mapper = CertificateSummaryDtoMapper()

    @Test
    fun `should map from Certificate to CertificatePrintRequestSummary given single print request with one status`() {
        // Given
        val vacNumber = aValidVacNumber()
        val expectedStatus = aValidCertificateStatus()
        val expectedDateTime = aValidRequestDateTime()
        val expectedUserId = aValidUserId()
        val certificate = buildCertificate(
            vacNumber = vacNumber,
            printRequests = listOf(
                buildPrintRequest(
                    userId = expectedUserId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(status = expectedStatus, eventDateTime = expectedDateTime, message = null)
                    )
                )
            )
        )
        val expected = CertificateSummaryDto(
            vacNumber = vacNumber,
            printRequests = listOf(
                PrintRequestSummaryDto(
                    status = PrintRequestStatusDto.valueOf(expectedStatus.name),
                    dateTime = expectedDateTime,
                    userId = expectedUserId,
                    message = null
                )
            )
        )

        // When
        val actual = mapper.certificateToCertificatePrintRequestSummaryDto(certificate)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map from Certificate to CertificatePrintRequestSummary given one print request with multiple status`() {
        // Given
        val vacNumber = aValidVacNumber()
        val expectedStatus = DISPATCHED
        val expectedDateTime = now().minusSeconds(2)
        val expectedMessage = "Success"
        val expectedUserId = aValidUserId()
        val certificate = buildCertificate(
            vacNumber = vacNumber,
            printRequests = listOf(
                buildPrintRequest(
                    userId = expectedUserId,
                    printRequestStatuses = listOf(
                        printRequestStatus(PENDING_ASSIGNMENT_TO_BATCH, now().minusSeconds(10), null),
                        printRequestStatus(ASSIGNED_TO_BATCH, now().minusSeconds(9), null),
                        printRequestStatus(SENT_TO_PRINT_PROVIDER, now().minusSeconds(8), null),
                        printRequestStatus(RECEIVED_BY_PRINT_PROVIDER, now().minusSeconds(7), null),
                        printRequestStatus(VALIDATED_BY_PRINT_PROVIDER, now().minusSeconds(6), null),
                        printRequestStatus(IN_PRODUCTION, now().minusSeconds(5), null),
                        printRequestStatus(expectedStatus, expectedDateTime, expectedMessage),
                    )
                )
            )
        )
        val expected = CertificateSummaryDto(
            vacNumber = vacNumber,
            printRequests = listOf(
                PrintRequestSummaryDto(
                    status = PrintRequestStatusDto.valueOf(expectedStatus.name),
                    dateTime = expectedDateTime,
                    userId = expectedUserId,
                    message = expectedMessage
                )
            )
        )

        // When
        val actual = mapper.certificateToCertificatePrintRequestSummaryDto(certificate)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map from Certificate to CertificatePrintRequestSummary given multiple print requests`() {
        // Given
        val vacNumber = aValidVacNumber()
        val expectedStatus1 = aValidCertificateStatus()
        val expectedDateTime1 = aValidRequestDateTime()
        val expectedUserId1 = aValidUserId()
        val expectedMessage1 = null
        val expectedStatus2 = aDifferentValidCertificateStatus()
        val expectedDateTime2 = aValidRequestDateTime()
        val expectedUserId2 = aValidUserId()
        val expectedMessage2 = "Successfully dispatched by Royal Mail"
        val certificate = buildCertificate(
            vacNumber = vacNumber,
            printRequests = listOf(
                buildPrintRequest(
                    userId = expectedUserId1,
                    printRequestStatuses = listOf(
                        printRequestStatus(expectedStatus1, expectedDateTime1, expectedMessage1)
                    )
                ),
                buildPrintRequest(
                    userId = expectedUserId2,
                    printRequestStatuses = listOf(
                        printRequestStatus(expectedStatus2, expectedDateTime2, expectedMessage2)
                    )
                )
            )
        )
        val expected = CertificateSummaryDto(
            vacNumber = vacNumber,
            printRequests = listOf(
                PrintRequestSummaryDto(
                    status = PrintRequestStatusDto.valueOf(expectedStatus1.name),
                    dateTime = expectedDateTime1,
                    userId = expectedUserId1,
                    message = expectedMessage1
                ),
                PrintRequestSummaryDto(
                    status = PrintRequestStatusDto.valueOf(expectedStatus2.name),
                    dateTime = expectedDateTime2,
                    userId = expectedUserId2,
                    message = expectedMessage2
                ),
            )
        )

        // When
        val actual = mapper.certificateToCertificatePrintRequestSummaryDto(certificate)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }

    private fun printRequestStatus(status: Status, eventDateTime: Instant, message: String?) =
        buildPrintRequestStatus(status = status, eventDateTime = eventDateTime, message = message)
}
