package uk.gov.dluhc.printapi.rds.entity

import org.assertj.core.api.Assertions.assertThat
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
    inner class PrePersist {

        @Test
        fun `should determine latest status for Certificate with no Print Requests`() {
            // Given
            val certificate = certificateBuilder(printRequests = listOf())

            // When
            certificate.prePersist()

            // Then
            assertThat(certificate.status).isEqualTo(Status.PENDING_ASSIGNMENT_TO_BATCH)
        }

        @Test
        fun `should determine latest status for Certificate with one Print Request with one status`() {
            // Given
            val certificate = certificateBuilder(
                printRequests = listOf(
                    printRequestBuilder(
                        printRequestStatuses = listOf(
                            printRequestStatusBuilder(
                                status = Status.ASSIGNED_TO_BATCH
                            )
                        )
                    )
                )
            )

            // When
            certificate.prePersist()

            // Then
            assertThat(certificate.status).isEqualTo(Status.ASSIGNED_TO_BATCH)
        }

        @Test
        fun `should determine latest status for Certificate with one Print Request with multiple statuses`() {
            // Given
            val certificate = certificateBuilder(
                listOf(
                    printRequestBuilder(
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
                )
            )

            // When
            certificate.prePersist()

            // Then
            assertThat(certificate.status).isEqualTo(Status.SENT_TO_PRINT_PROVIDER)
        }

        @Test
        fun `should determine latest status for Certificate with multiple Print Requests with multiple statuses`() {
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
            val certificate = certificateBuilder(listOf(firstPrintRequest, secondPrintRequest))

            // When
            certificate.prePersist()

            // Then
            assertThat(certificate.status).isEqualTo(Status.DISPATCHED)
        }
    }
}
