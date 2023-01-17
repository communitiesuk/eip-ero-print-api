package uk.gov.dluhc.printapi.database.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
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
    inner class AddPrintRequestToBatch {
        @Test
        fun `should addPrintRequestToBatch for Certificate with one Print Request PENDING_ASSIGNMENT_TO_BATCH`() {
            // Given
            val batchId = aValidBatchId()
            val pendingAssignmentPrintRequest = buildPrintRequest(
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minusSeconds(1)
                    )
                )
            )
            val certificate = buildCertificate(printRequests = listOf(pendingAssignmentPrintRequest))
            val expectedStatus = Status.ASSIGNED_TO_BATCH

            // When
            certificate.addPrintRequestToBatch(batchId)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(pendingAssignmentPrintRequest.getCurrentStatus().status).isEqualTo(expectedStatus)
            assertThat(pendingAssignmentPrintRequest.batchId).isEqualTo(batchId)
        }

        @Test
        fun `should addPrintRequestToBatch for Certificate with multiple Print Requests including one PENDING_ASSIGNMENT_TO_BATCH`() {
            // Given
            val batchId = aValidBatchId()
            val failedPrintRequest = buildPrintRequest(
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
            val printRequestToIncludeInBatch = buildPrintRequest(
                requestDateTime = Instant.now().minus(1, ChronoUnit.DAYS),
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(1, ChronoUnit.DAYS)
                    )
                )
            )
            val certificate = buildCertificate(printRequests = listOf(failedPrintRequest, printRequestToIncludeInBatch))
            val expectedStatus = Status.ASSIGNED_TO_BATCH

            // When
            certificate.addPrintRequestToBatch(batchId)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(printRequestToIncludeInBatch.getCurrentStatus().status).isEqualTo(expectedStatus)
            assertThat(printRequestToIncludeInBatch.batchId).isEqualTo(batchId)
        }
    }

    @Nested
    inner class AddSentToPrintProviderEventForBatch {
        @Test
        fun `should addSentToPrintProviderEventForBatch for Certificate with one Print Request assigned to batch`() {
            // Given
            val batchId = aValidBatchId()
            val assignedToBatchPrintRequest = buildPrintRequest(
                batchId = batchId,
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minusSeconds(1)
                    )
                )
            )
            val certificate = buildCertificate(printRequests = listOf(assignedToBatchPrintRequest))
            val expectedStatus = Status.SENT_TO_PRINT_PROVIDER

            // When
            certificate.addSentToPrintProviderEventForBatch(batchId)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(assignedToBatchPrintRequest.getCurrentStatus().status).isEqualTo(expectedStatus)
            assertThat(assignedToBatchPrintRequest.batchId).isEqualTo(batchId)
        }

        @Test
        fun `should addSentToPrintProviderEventForBatch for Certificate with multiple Print Requests including one assigned to batch`() {
            // Given
            val oldBatchId = aValidBatchId()
            val batchIdBeingProcessed = aValidBatchId()
            val oldPrintRequest = buildPrintRequest(
                requestDateTime = Instant.now().minus(30, ChronoUnit.DAYS),
                batchId = oldBatchId,
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
            val printRequestAssignedToBatch = buildPrintRequest(
                requestDateTime = Instant.now().minus(1, ChronoUnit.DAYS),
                batchId = batchIdBeingProcessed,
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(10, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.ASSIGNED_TO_BATCH,
                        eventDateTime = Instant.now().minus(9, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = buildCertificate(printRequests = listOf(oldPrintRequest, printRequestAssignedToBatch))
            val expectedStatus = Status.SENT_TO_PRINT_PROVIDER

            // When
            certificate.addSentToPrintProviderEventForBatch(batchIdBeingProcessed)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(printRequestAssignedToBatch.getCurrentStatus().status).isEqualTo(expectedStatus)
            assertThat(printRequestAssignedToBatch.batchId).isEqualTo(batchIdBeingProcessed)
        }
    }

    @Nested
    inner class AddReceivedByPrintProviderEventForBatch {
        @Test
        fun `should add RECEIVED_BY_PRINT_PROVIDER for Batch for Certificate with one Print Request SENT_TO_PRINT_PROVIDER`() {
            // Given
            val batchId = aValidBatchId()
            val pendingAssignmentPrintRequest = buildPrintRequest(
                batchId = batchId,
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minusSeconds(1)
                    )
                )
            )
            val certificate = buildCertificate(printRequests = listOf(pendingAssignmentPrintRequest))
            val expectedStatus = Status.RECEIVED_BY_PRINT_PROVIDER
            val expectedEvent = buildPrintRequestStatus(status = expectedStatus)

            // When
            certificate.addReceivedByPrintProviderEventForBatch(batchId, expectedEvent.eventDateTime!!, expectedEvent.message)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(pendingAssignmentPrintRequest.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
            assertThat(pendingAssignmentPrintRequest.batchId).isEqualTo(batchId)
        }

        @Test
        fun `should add RECEIVED_BY_PRINT_PROVIDER for Batch for Certificate with multiple Print Requests including one SENT_TO_PRINT_PROVIDER`() {
            // Given
            val failedBatchId = aValidBatchId()
            val resendBatchId = aValidBatchId()
            val failedPrintRequest = buildPrintRequest(
                requestDateTime = Instant.now().minus(30, ChronoUnit.DAYS),
                batchId = failedBatchId,
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
            val resendPrintRequest = buildPrintRequest(
                requestDateTime = Instant.now().minus(1, ChronoUnit.DAYS),
                batchId = resendBatchId,
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(1, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(28, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = buildCertificate(printRequests = listOf(failedPrintRequest, resendPrintRequest))
            val expectedStatus = Status.RECEIVED_BY_PRINT_PROVIDER
            val expectedEvent = buildPrintRequestStatus(status = expectedStatus)

            // When
            certificate.addReceivedByPrintProviderEventForBatch(resendBatchId, expectedEvent.eventDateTime!!, expectedEvent.message)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(resendPrintRequest.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
            assertThat(resendPrintRequest.batchId).isEqualTo(resendBatchId)
        }
    }

    @Nested
    inner class RequeuePrintRequestForBatch {
        @Test
        fun `should requeuePrintRequestForBatch for Certificate with one Print Request SENT_TO_PRINT_PROVIDER`() {
            // Given
            val batchId = aValidBatchId()
            val newRequestId = aValidRequestId()
            val pendingAssignmentPrintRequest = buildPrintRequest(
                batchId = batchId,
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minusSeconds(1)
                    )
                )
            )
            val certificate = buildCertificate(printRequests = listOf(pendingAssignmentPrintRequest))
            val expectedStatus = Status.PENDING_ASSIGNMENT_TO_BATCH
            val expectedEvent = buildPrintRequestStatus(status = expectedStatus)

            // When
            certificate.requeuePrintRequestForBatch(batchId, expectedEvent.eventDateTime!!, expectedEvent.message, newRequestId)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(pendingAssignmentPrintRequest.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
            assertThat(pendingAssignmentPrintRequest.batchId).isNull()
            assertThat(pendingAssignmentPrintRequest.requestId).isEqualTo(newRequestId)
        }

        @Test
        fun `should requeuePrintRequestForBatch for Certificate with multiple Print Requests including one SENT_TO_PRINT_PROVIDER`() {
            // Given
            val oldBatchId = aValidBatchId()
            val batchIdBeingProcessed = aValidBatchId()
            val newRequestId = aValidRequestId()
            val oldPrintRequest = buildPrintRequest(
                requestDateTime = Instant.now().minus(30, ChronoUnit.DAYS),
                batchId = oldBatchId,
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
            val printRequestBeingProcessed = buildPrintRequest(
                requestDateTime = Instant.now().minus(1, ChronoUnit.DAYS),
                batchId = batchIdBeingProcessed,
                printRequestStatuses = listOf(
                    buildPrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minus(1, ChronoUnit.DAYS)
                    ),
                    buildPrintRequestStatus(
                        status = Status.SENT_TO_PRINT_PROVIDER,
                        eventDateTime = Instant.now().minus(28, ChronoUnit.DAYS)
                    ),
                )
            )
            val certificate = buildCertificate(printRequests = listOf(oldPrintRequest, printRequestBeingProcessed))
            val expectedStatus = Status.PENDING_ASSIGNMENT_TO_BATCH
            val expectedEvent = buildPrintRequestStatus(status = expectedStatus)

            // When
            certificate.requeuePrintRequestForBatch(batchIdBeingProcessed, expectedEvent.eventDateTime!!, expectedEvent.message, newRequestId)

            // Then
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(printRequestBeingProcessed.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
            assertThat(printRequestBeingProcessed.batchId).isNull()
            assertThat(printRequestBeingProcessed.requestId).isEqualTo(newRequestId)
        }
    }

    @Nested
    inner class UpdatePrintRequestStatusByRequestId {

        @Test
        fun `should update status for Certificate with one Print Request with one status`() {
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
            val expectedEvent = buildPrintRequestStatus(status = status)

            // When
            certificate.addPrintRequestEvent(printRequest.requestId!!, status, expectedEvent.eventDateTime!!, expectedEvent.message)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(printRequest.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
        }

        @Test
        fun `should update status for Certificate with one Print Request with multiple statuses`() {
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
            val expectedEvent = buildPrintRequestStatus(status = status)

            // When
            certificate.addPrintRequestEvent(printRequest.requestId!!, status, expectedEvent.eventDateTime!!, expectedEvent.message)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(printRequest.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
        }

        @Test
        fun `should update status for Certificate with multiple Print Requests with multiple statuses`() {
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
            val expectedEvent = buildPrintRequestStatus(status = status)

            // When
            certificate.addPrintRequestEvent(secondPrintRequest.requestId!!, status, expectedEvent.eventDateTime!!, expectedEvent.message)

            // Then
            assertThat(certificate.status).isEqualTo(status)
            assertThat(secondPrintRequest.getCurrentStatus()).usingRecursiveComparison().isEqualTo(expectedEvent)
        }
    }
}
