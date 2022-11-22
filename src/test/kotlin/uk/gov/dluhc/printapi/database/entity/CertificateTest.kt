package uk.gov.dluhc.printapi.database.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class CertificateTest {

    @Nested
    inner class GetCurrentPrintRequest {

        @Test
        fun `should fail to get latest print request for Certificate with no Print Requests`() {
            // Given
            val certificate = buildCertificate(printRequests = emptyList())

            // When
            val actual = catchException { certificate.getCurrentPrintRequest() }

            // Then
            assertThat(actual).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `should get latest status for Certificate with one Print Request`() {
            // Given
            val currentPrintRequest = buildPrintRequest()
            val certificate = buildCertificate(printRequests = listOf(currentPrintRequest))

            // When
            val actual = certificate.getCurrentPrintRequest()

            // Then
            assertThat(actual).isSameAs(currentPrintRequest)
        }

        @Test
        fun `should determine latest status for Certificate with one Print Request with multiple statuses`() {
            // Given
            val previousPrintRequest = buildPrintRequest(requestDateTime = Instant.now().minus(8, ChronoUnit.DAYS))
            val currentPrintRequest = buildPrintRequest(requestDateTime = Instant.now().minus(3, ChronoUnit.DAYS))
            val certificate = buildCertificate(printRequests = listOf(currentPrintRequest, previousPrintRequest))

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
            val certificate = buildCertificate(printRequests = emptyList())
            val status = Status.VALIDATED_BY_PRINT_PROVIDER

            // When
            val actual = catchException { certificate.addStatus(status) }

            // Then
            assertThat(actual).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `should add status for Certificate with one Print Request with one status`() {
            // Given
            val printRequest = buildPrintRequest(
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minusSeconds(1)
                    )
                )
            )
            val certificate = buildCertificate(printRequests = listOf(printRequest))
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
            val printRequest = buildPrintRequest(
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(10, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(9, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(8, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = buildCertificate(printRequests = listOf(printRequest))
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
            val firstPrintRequest = buildPrintRequest(
                requestDateTime = Instant.now().minus(30, ChronoUnit.DAYS),
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(30, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(29, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(28, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.PRINT_PROVIDER_DISPATCH_FAILED,
                        eventDateTime = Instant.now().minus(20, ChronoUnit.DAYS)
                    ),
                )
            )

            val secondPrintRequest = buildPrintRequest(
                requestDateTime = Instant.now().minus(10, ChronoUnit.DAYS),
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(10, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(9, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(8, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.DISPATCHED,
                        eventDateTime = Instant.now().minus(3, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = buildCertificate(printRequests = listOf(firstPrintRequest, secondPrintRequest))
            val status = Status.VALIDATED_BY_PRINT_PROVIDER

            // When
            certificate.addStatus(status)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(secondPrintRequest.statusHistory).extracting(PrintRequestStatus::status).contains(Tuple(status))
        }
    }
}
