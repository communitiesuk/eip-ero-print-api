package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.mapper.ProcessPrintResponseMessageMapper
import uk.gov.dluhc.printapi.mapper.StatusMapper
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.FAILED
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildProcessPrintResponseMessage
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class PrintResponseProcessingServiceTest {
    private lateinit var service: PrintResponseProcessingService

    @Mock
    private lateinit var printDetailsRepository: PrintDetailsRepository

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var statusMapper: StatusMapper

    @Mock
    private lateinit var processPrintResponseMessageMapper: ProcessPrintResponseMessageMapper

    @Mock
    private lateinit var processPrintResponseQueue: MessageQueue<ProcessPrintResponseMessage>

    private val fixedClock = Clock.fixed(Instant.now().minusSeconds(10000), ZoneOffset.UTC)

    @Captor
    private lateinit var captor: ArgumentCaptor<List<PrintDetails>>

    @Captor
    private lateinit var processPrintResponseMessageCaptor: ArgumentCaptor<ProcessPrintResponseMessage>

    private val now = OffsetDateTime.now(fixedClock)

    @BeforeEach
    fun setup() {
        service = PrintResponseProcessingService(
            printDetailsRepository,
            idFactory,
            fixedClock,
            statusMapper,
            processPrintResponseMessageMapper,
            processPrintResponseQueue
        )
    }

    @Nested
    inner class ProcessBatchAndPrintResponses {
        @Test
        fun `should process batch responses and queue print responses`() {
            // Given
            val responses = buildPrintResponses()
            val batchResponses = responses.batchResponses
            val printResponses = responses.printResponses
            val processingService = spy(service)
            doNothing().`when`(processingService).processBatchResponses(any())
            val messages = printResponses.map {
                val message = buildProcessPrintResponseMessage(
                    requestId = it.requestId,
                    timestamp = it.timestamp,
                    message = it.message
                )
                given(processPrintResponseMessageMapper.toProcessPrintResponseMessage(it)).willReturn(message)
                message
            }

            // When
            processingService.processBatchAndPrintResponses(responses)

            // Then
            val inOrder = inOrder(processingService, processPrintResponseQueue)
            inOrder.verify(processingService).processBatchResponses(batchResponses)
            inOrder.verify(processPrintResponseQueue).submit(capture(processPrintResponseMessageCaptor))
            val values = processPrintResponseMessageCaptor.allValues
            assertThat(values).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(messages)
        }
    }

    @Nested
    inner class ProcessBatchResponses {
        @Test
        fun `should update all print requests within each batch`() {
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
                message = batchResponse1.message
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
                batchId = null
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
        @Test
        fun `should update print request given valid statusStep and status`() {
            // Given
            val printDetails = buildPrintDetails()
            val requestId = printDetails.requestId!!
            given(printDetailsRepository.getByRequestId(any())).willReturn(printDetails)
            val response = buildProcessPrintResponseMessage(requestId = requestId)
            val expectedStatus = IN_PRODUCTION
            given(statusMapper.toStatusEntityEnum(any(), any())).willReturn(expectedStatus)
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
            verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
            verify(printDetailsRepository).getByRequestId(requestId)
            verify(printDetailsRepository).updateItems(capture(captor))
            assertThat(captor.value).usingRecursiveComparison().isEqualTo(listOf(expectedPrintDetails))
        }
    }

    @Test
    fun `should throw exception given statusMapper throws exception`() {
        // Given
        val response = buildProcessPrintResponseMessage()
        val exception = IllegalArgumentException("Undefined statusStep and status combination")
        given(statusMapper.toStatusEntityEnum(any(), any())).willThrow(exception)

        // When
        val ex = Assertions.catchThrowableOfType(
            { service.processPrintResponse(response) },
            IllegalArgumentException::class.java
        )

        // Then
        assertThat(ex).isSameAs(exception)
        verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
        verifyNoInteractions(printDetailsRepository)
    }
}
