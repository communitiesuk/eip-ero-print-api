package uk.gov.dluhc.printapi.service

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
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
import org.mockito.kotlin.never
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
import uk.gov.dluhc.printapi.database.repository.PrintDetailsNotFoundException
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.mapper.ProcessPrintResponseMessageMapper
import uk.gov.dluhc.printapi.mapper.StatusMapper
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.FAILED
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildProcessPrintResponseMessage
import uk.gov.dluhc.printapi.testsupport.testdata.rds.certificateBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestStatusBuilder
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
    private lateinit var certificateRepository: CertificateRepository

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
            certificateRepository,
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

        @Test
        fun `should update all certificates within each batch`() {
            // Given
            val batchResponse1 = buildBatchResponse(status = FAILED)
            val batchResponse2 = buildBatchResponse(status = SUCCESS)
            val batchId1 = batchResponse1.batchId
            val batchId2 = batchResponse2.batchId
            val certificate1 = certificateBuilder(
                printRequests = listOf(
                    printRequestBuilder(
                        batchId = batchId1,
                        printRequestStatuses = listOf(
                            printRequestStatusBuilder(
                                status = SENT_TO_PRINT_PROVIDER,
                                eventDateTime = batchResponse1.timestamp.toInstant().minusSeconds(10)
                            )
                        )
                    )
                )
            )
            val certificate2 = certificateBuilder(
                printRequests = listOf(
                    printRequestBuilder(
                        batchId = batchId2,
                        printRequestStatuses = listOf(
                            printRequestStatusBuilder(
                                status = SENT_TO_PRINT_PROVIDER,
                                eventDateTime = batchResponse2.timestamp.toInstant().minusSeconds(10)
                            )
                        )
                    )
                )
            )

            val newRequestId = aValidRequestId()
            given(certificateRepository.findByStatusAndPrintRequestsBatchId(any(), any()))
                .willReturn(listOf(certificate1), listOf(certificate2))
            given(idFactory.requestId()).willReturn(newRequestId)

            // When
            service.processBatchResponses(listOf(batchResponse1, batchResponse2))

            // Then
            verify(certificateRepository).findByStatusAndPrintRequestsBatchId(
                SENT_TO_PRINT_PROVIDER,
                batchResponse1.batchId
            )
            verify(certificateRepository).findByStatusAndPrintRequestsBatchId(
                SENT_TO_PRINT_PROVIDER,
                batchResponse2.batchId
            )
            verify(certificateRepository).saveAll(listOf(certificate1))
            verify(certificateRepository).saveAll(listOf(certificate2))
            assertThat(certificate1.getCurrentPrintRequest().requestId).isEqualTo(newRequestId)
            assertThat(certificate1.getCurrentPrintRequest().batchId).isNull()
            assertThat(
                certificate1.getCurrentPrintRequest().statusHistory.sortedByDescending { it.eventDateTime }
                    .first()
            )
                .usingRecursiveComparison().isEqualTo(
                    uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus(
                        status = PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = batchResponse1.timestamp.toInstant(),
                        message = batchResponse1.message
                    )
                )
            assertThat(
                certificate2.getCurrentPrintRequest().statusHistory.sortedByDescending { it.eventDateTime }
                    .first()
            )
                .usingRecursiveComparison().isEqualTo(
                    uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus(
                        status = RECEIVED_BY_PRINT_PROVIDER,
                        eventDateTime = batchResponse2.timestamp.toInstant(),
                        message = batchResponse2.message
                    )
                )
            verify(idFactory).requestId()
        }
    }

    @Nested
    inner class ProcessPrintResponse {
        @Test
        fun `should update print request given existing printDetails and valid statusStep and status`() {
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

            val certificate = certificateBuilder(
                printRequests = listOf(
                    printRequestBuilder(
                        requestId = requestId,
                        printRequestStatuses = listOf(
                            printRequestStatusBuilder(
                                status = SENT_TO_PRINT_PROVIDER,
                                eventDateTime = response.timestamp.toInstant().minusSeconds(10)
                            )
                        )
                    )
                )
            )
            given(certificateRepository.getByPrintRequestsRequestId(any())).willReturn(certificate)

            // When
            service.processPrintResponse(response)

            // Then
            verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
            verify(printDetailsRepository).getByRequestId(requestId)
            verify(certificateRepository).getByPrintRequestsRequestId(requestId)
            verify(printDetailsRepository).updateItems(capture(captor))
            assertThat(captor.value).usingRecursiveComparison().isEqualTo(listOf(expectedPrintDetails))
            verify(certificateRepository).save(certificate)
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(
                certificate.getCurrentPrintRequest().statusHistory.sortedByDescending { it.eventDateTime }
                    .first()
            )
                .usingRecursiveComparison().isEqualTo(
                    uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus(
                        status = expectedStatus,
                        eventDateTime = response.timestamp.toInstant(),
                        message = response.message
                    )
                )
        }

        @Test
        fun `should log and not throw exception given no print details for the requestId`() {
            // Given
            val response = buildProcessPrintResponseMessage()
            val requestId = response.requestId
            val exception = PrintDetailsNotFoundException(requestId)
            given(statusMapper.toStatusEntityEnum(any(), any())).willReturn(IN_PRODUCTION)
            given(printDetailsRepository.getByRequestId(any())).willThrow(exception)
            given(certificateRepository.getByPrintRequestsRequestId(any())).willReturn(null)
            TestLogAppender.reset()

            // When
            service.processPrintResponse(response)

            // Then
            assertThat(TestLogAppender.hasLog(exception.message!!, Level.ERROR)).isTrue
            assertThat(TestLogAppender.hasLog("Certificate not found for the requestId $requestId", Level.ERROR)).isTrue
            verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
            verify(printDetailsRepository).getByRequestId(requestId)
            verify(printDetailsRepository, never()).updateItems(any())
            verify(certificateRepository).getByPrintRequestsRequestId(requestId)
            verify(certificateRepository, never()).save(any())
        }

        @Test
        fun `should throw exception given an error in the dynamodb`() {
            // Given
            val response = buildProcessPrintResponseMessage()
            val requestId = response.requestId
            val exception = RuntimeException("Some error occurred in the db")
            given(statusMapper.toStatusEntityEnum(any(), any())).willReturn(IN_PRODUCTION)
            given(printDetailsRepository.getByRequestId(any())).willThrow(exception)

            // When
            val ex = catchThrowableOfType({
                service.processPrintResponse(response)
            }, Exception::class.java)

            // Then
            assertThat(ex).isSameAs(exception)
            verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
            verify(printDetailsRepository).getByRequestId(requestId)
            verify(printDetailsRepository, never()).updateItems(any())
            verifyNoInteractions(certificateRepository)
        }

        @Test
        fun `should log and not throw exception given statusMapper throws exception`() {
            // Given
            val response = buildProcessPrintResponseMessage()
            val message = "Undefined statusStep and status combination"
            val exception = IllegalArgumentException(message)
            given(statusMapper.toStatusEntityEnum(any(), any())).willThrow(exception)
            TestLogAppender.reset()

            // When
            service.processPrintResponse(response)

            // Then
            assertThat(TestLogAppender.hasLog(message, Level.ERROR)).isTrue
            verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
            verifyNoInteractions(printDetailsRepository)
            verifyNoInteractions(certificateRepository)
        }
    }
}
