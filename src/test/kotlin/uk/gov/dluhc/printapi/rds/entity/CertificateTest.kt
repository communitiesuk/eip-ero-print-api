package uk.gov.dluhc.printapi.rds.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.testdata.rds.certificateBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestStatusBuilder
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class CertificateTest {

    @Nested
    inner class GetCurrentPrintRequest {

        @Test
        fun `should fail to get latest print request for Certificate with no Print Requests`() {
            // Given
            val certificate = certificateBuilder(printRequests = emptyList())

            // When
            val actual = catchException { certificate.getCurrentPrintRequest() }

            // Then
            assertThat(actual).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `should get latest status for Certificate with one Print Request`() {
            // Given
            val currentPrintRequest = printRequestBuilder()
            val certificate = certificateBuilder(printRequests = listOf(currentPrintRequest))

            // When
            val actual = certificate.getCurrentPrintRequest()

            // Then
            assertThat(actual).isSameAs(currentPrintRequest)
        }

        @Test
        fun `should determine latest status for Certificate with one Print Request with multiple statuses`() {
            // Given
            val previousPrintRequest = printRequestBuilder(requestDateTime = Instant.now().minus(8, ChronoUnit.DAYS))
            val currentPrintRequest = printRequestBuilder(requestDateTime = Instant.now().minus(3, ChronoUnit.DAYS))
            val certificate = certificateBuilder(printRequests = listOf(currentPrintRequest, previousPrintRequest))

            // When
            val actual = certificate.getCurrentPrintRequest()

            // Then
            assertThat(actual).isSameAs(currentPrintRequest)
        }
    }

    @Nested
    inner class AddStatus {

        @Test
        fun `should fail to add status for Certificate with no existing Print Requests`() {
            // Given
            val certificate = certificateBuilder(printRequests = emptyList())
            val status = Status.VALIDATED_BY_PRINT_PROVIDER

            // When
            val actual = catchException { certificate.addStatus(status) }

            // Then
            assertThat(actual).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `should add status for Certificate with one Print Request with one status`() {
            // Given
            val printRequest = printRequestBuilder(
                printRequestStatuses = listOf(
                    printRequestStatusBuilder(
                        status = Status.ASSIGNED_TO_BATCH
                    )
                )
            )
            val certificate = certificateBuilder(printRequests = listOf(printRequest))
            val status = Status.VALIDATED_BY_PRINT_PROVIDER

            // When
            certificate.addStatus(status)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(printRequest.statusHistory).extracting(PrintRequestStatus::status).contains(Tuple(status))
        }

        @Test
        fun `should add status for Certificate with one Print Request with multiple statuses`() {
            // Given
            val printRequest = printRequestBuilder(
                printRequestStatuses = listOf(
                    printRequestStatusBuilder(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(10, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(9, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(8, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = certificateBuilder(printRequests = listOf(printRequest))
            val status = Status.VALIDATED_BY_PRINT_PROVIDER

            // When
            certificate.addStatus(status)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(printRequest.statusHistory).extracting(PrintRequestStatus::status).contains(Tuple(status))
        }

        @Test
        fun `should add status for Certificate with multiple Print Requests with multiple statuses`() {
            // Given
            val firstPrintRequest = printRequestBuilder(
                requestDateTime = Instant.now().minus(30, ChronoUnit.DAYS),
                printRequestStatuses = listOf(
                    printRequestStatusBuilder(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(30, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(29, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(28, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.PRINT_PROVIDER_DISPATCH_FAILED,
                        eventDateTime = Instant.now().minus(20, ChronoUnit.DAYS)
                    ),
                )
            )

            val secondPrintRequest = printRequestBuilder(
                requestDateTime = Instant.now().minus(10, ChronoUnit.DAYS),
                printRequestStatuses = listOf(
                    printRequestStatusBuilder(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(10, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(9, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(8, ChronoUnit.DAYS)
                    ),
                    printRequestStatusBuilder(
                        status = Status.DISPATCHED,
                        eventDateTime = Instant.now().minus(3, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = certificateBuilder(printRequests = listOf(firstPrintRequest, secondPrintRequest))
            val status = Status.VALIDATED_BY_PRINT_PROVIDER

            // When
            certificate.addStatus(status)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(secondPrintRequest.statusHistory).extracting(PrintRequestStatus::status).contains(Tuple(status))
        }
    }
}
