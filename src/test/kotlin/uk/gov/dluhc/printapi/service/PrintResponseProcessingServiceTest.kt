package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.Status.DISPATCHED
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.FAILED
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildProcessPrintResponseMessage
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.Status as ResponseStatus
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage.StatusStep as ResponseStatusStep

@ExtendWith(MockitoExtension::class)
class PrintResponseProcessingServiceTest {
    private lateinit var service: PrintResponseProcessingService

    @Mock
    private lateinit var printDetailsRepository: PrintDetailsRepository

    @Mock
    private lateinit var idFactory: IdFactory

    private val fixedClock = Clock.fixed(Instant.now().minusSeconds(10000), ZoneOffset.UTC)

    @Captor
    private lateinit var captor: ArgumentCaptor<List<PrintDetails>>

    private val now = OffsetDateTime.now(fixedClock)

    @BeforeEach
    fun setup() {
        service = PrintResponseProcessingService(printDetailsRepository, idFactory, fixedClock)
    }

    @Nested
    inner class ProcessBatchResponses {
        @Test
        fun `should update print details`() {
            // Given
            val batchResponse1 = buildBatchResponse(status = FAILED)
            val batchResponse2 = buildBatchResponse(status = SUCCESS)
            val batchId1 = batchResponse1.batchId
            val batchId2 = batchResponse2.batchId

            val printDetails1 = buildPrintDetails(batchId = batchId1, status = SENT_TO_PRINT_PROVIDER)
            val printDetails2 = buildPrintDetails(batchId = batchId2, status = SENT_TO_PRINT_PROVIDER)
            val newRequestId = aValidRequestId()
            given(printDetailsRepository.getAllByStatusAndBatchId(any(), any())).willReturn(
                listOf(printDetails1),
                listOf(printDetails2)
            )
            given(idFactory.requestId()).willReturn(newRequestId)

            val expectedPrintRequestStatus1 = PrintRequestStatus(
                status = PENDING_ASSIGNMENT_TO_BATCH,
                dateCreated = now,
                eventDateTime = batchResponse1.timestamp,
                message = null
            )
            val expectedPrintRequestStatus2 = PrintRequestStatus(
                status = RECEIVED_BY_PRINT_PROVIDER,
                dateCreated = now,
                eventDateTime = batchResponse2.timestamp,
                message = null
            )
            val expectedPrintDetails1 = printDetails1.copy().apply {
                printRequestStatuses = printRequestStatuses!!.toMutableList().apply { add(expectedPrintRequestStatus1) }
                requestId = newRequestId
            }
            val expectedPrintDetails2 = printDetails2.copy().apply {
                printRequestStatuses = printRequestStatuses!!.toMutableList().apply { add(expectedPrintRequestStatus2) }
            }

            // When
            service.processBatchResponses(listOf(batchResponse1, batchResponse2))

            // Then
            verify(printDetailsRepository).getAllByStatusAndBatchId(SENT_TO_PRINT_PROVIDER, batchResponse1.batchId)
            verify(printDetailsRepository).getAllByStatusAndBatchId(SENT_TO_PRINT_PROVIDER, batchResponse2.batchId)
            verify(printDetailsRepository, times(2)).updateItems(capture(captor))
            assertThat(captor.allValues[0]).usingRecursiveComparison().isEqualTo(listOf(expectedPrintDetails1))
            assertThat(captor.allValues[1]).usingRecursiveComparison().isEqualTo(listOf(expectedPrintDetails2))
            verify(idFactory).requestId()
        }
    }

    @Nested
    inner class ProcessPrintResponse {
        @ParameterizedTest
        @CsvSource(
            value = [
                "RECEIVED_BY_PRINT_PROVIDER, PROCESSED, SUCCESS, VALIDATED_BY_PRINT_PROVIDER",
                "VALIDATED_BY_PRINT_PROVIDER, IN_MINUS_PRODUCTION, SUCCESS, IN_PRODUCTION",
                "IN_PRODUCTION, DISPATCHED, SUCCESS, DISPATCHED",
                "DISPATCHED, NOT_MINUS_DELIVERED, FAILED, NOT_DELIVERED",
                "RECEIVED_BY_PRINT_PROVIDER, PROCESSED, FAILED, PRINT_PROVIDER_VALIDATION_FAILED",
                "VALIDATED_BY_PRINT_PROVIDER, IN_MINUS_PRODUCTION, FAILED, PRINT_PROVIDER_PRODUCTION_FAILED  ",
                "IN_PRODUCTION, DISPATCHED, FAILED, PRINT_PROVIDER_DISPATCH_FAILED"
            ]
        )
        fun `should update print details`(
            initialStatus: Status,
            statusStep: ResponseStatusStep,
            status: ResponseStatus,
            expectedStatus: Status
        ) {
            // Given
            val printDetails = buildPrintDetails(status = initialStatus)
            val requestId = printDetails.requestId!!
            given(printDetailsRepository.getByRequestId(any())).willReturn(printDetails)
            val response = buildProcessPrintResponseMessage(
                requestId = requestId,
                statusStep = statusStep,
                status = status
            )
            val expectedPrintRequestStatus = PrintRequestStatus(
                status = expectedStatus,
                dateCreated = now,
                eventDateTime = response.timestamp,
                message = response.message
            )
            val expectedPrintDetails = printDetails.copy().apply {
                printRequestStatuses = printRequestStatuses!!.toMutableList().apply { add(expectedPrintRequestStatus) }
            }
            // When
            service.processPrintResponse(response)

            // Then
            verify(printDetailsRepository).getByRequestId(requestId)
            verify(printDetailsRepository).updateItems(capture(captor))
            assertThat(captor.value).usingRecursiveComparison().isEqualTo(listOf(expectedPrintDetails))
        }
    }

    @Test
    fun `should throw exception given non defined statusStep and status combination`() {
        // Given
        val printDetails = buildPrintDetails(status = DISPATCHED)
        val requestId = printDetails.requestId!!
        val response = buildProcessPrintResponseMessage(
            requestId = requestId,
            statusStep = ResponseStatusStep.NOT_MINUS_DELIVERED,
            status = ResponseStatus.SUCCESS
        )

        // When
        val ex = Assertions.catchThrowableOfType(
            { service.processPrintResponse(response) },
            IllegalArgumentException::class.java
        )

        // Then
        assertThat(ex).isNotNull.hasMessage("Undefined statusStep [NOT_MINUS_DELIVERED] and status [SUCCESS] combination")
        verifyNoInteractions(printDetailsRepository)
    }
}
