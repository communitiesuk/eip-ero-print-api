package uk.gov.dluhc.printapi.database.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class PrintRequestTest {

    @Nested
    inner class GetCurrentPrintRequest {

        @Test
        fun `should fail to get current status for Print Request with no Print Statuses`() {
            // Given
            val printRequest = buildPrintRequest(printRequestStatuses = emptyList())

            // When
            val actual = catchException { printRequest.getCurrentStatus() }

            // Then
            assertThat(actual).isInstanceOf(NoSuchElementException::class.java)
        }

        @Test
        fun `should get current status for Print Request with one Print Request Status`() {
            // Given
            val currentPrintRequestStatus = buildPrintRequestStatus()
            val printRequest = buildPrintRequest(printRequestStatuses = listOf(currentPrintRequestStatus))

            // When
            val actual = printRequest.getCurrentStatus()

            // Then
            assertThat(actual).isSameAs(currentPrintRequestStatus)
        }

        @Test
        fun `should determine latest status for Print Request with multiple statuses`() {
            // Given
            val previousPrintRequest = buildPrintRequestStatus(eventDateTime = Instant.now().minus(8, ChronoUnit.DAYS))
            val currentPrintRequestStatus =
                buildPrintRequestStatus(eventDateTime = Instant.now().minus(3, ChronoUnit.DAYS))
            val printRequest =
                buildPrintRequest(printRequestStatuses = listOf(previousPrintRequest, currentPrintRequestStatus))

            // When
            val actual = printRequest.getCurrentStatus()

            // Then
            assertThat(actual).isSameAs(currentPrintRequestStatus)
        }

        @RepeatedTest(20)
        fun `should determine latest status for Print Request with events sharing same event timestamp`() {
            // Given
            val eventDateTime = Instant.now()
            val previousPrintRequest =
                buildPrintRequestStatus(eventDateTime = eventDateTime, status = PENDING_ASSIGNMENT_TO_BATCH)
            val currentPrintRequestStatus =
                buildPrintRequestStatus(eventDateTime = eventDateTime, status = ASSIGNED_TO_BATCH)
            val printRequest =
                buildPrintRequest(
                    printRequestStatuses = listOf(
                        previousPrintRequest,
                        currentPrintRequestStatus
                    ).shuffled()
                )

            // When
            val actual = printRequest.getCurrentStatus()

            // Then
            assertThat(actual).isSameAs(currentPrintRequestStatus)
        }
    }
}
