package uk.gov.dluhc.printapi.service

import ch.qos.logback.classic.Level
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
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status.IN_PRODUCTION
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.ProcessPrintResponseMessageMapper
import uk.gov.dluhc.printapi.mapper.StatusMapper
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.FAILED
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildProcessPrintResponseMessage

@ExtendWith(MockitoExtension::class)
class PrintResponseProcessingServiceTest {
    private lateinit var service: PrintResponseProcessingService

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

    @Captor
    private lateinit var processPrintResponseMessageCaptor: ArgumentCaptor<ProcessPrintResponseMessage>

    @BeforeEach
    fun setup() {
        service = PrintResponseProcessingService(
            certificateRepository,
            idFactory,
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
            val printResponses = listOf(buildPrintResponse())
            val processingService = spy(service)
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
            processingService.processPrintResponses(printResponses)

            // Then
            val inOrder = inOrder(processingService, processPrintResponseQueue)
            inOrder.verify(processPrintResponseQueue).submit(capture(processPrintResponseMessageCaptor))
            val values = processPrintResponseMessageCaptor.allValues
            assertThat(values).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(messages)
        }
    }

    @Nested
    inner class ProcessBatchResponses {
        @Test
        fun `should update all certificates within each batch`() {
            // Given
            val batchResponse1 = buildBatchResponse(status = FAILED)
            val batchResponse2 = buildBatchResponse(status = SUCCESS)
            val batchId1 = batchResponse1.batchId
            val batchId2 = batchResponse2.batchId
            val certificate1 = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        batchId = batchId1,
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
                                status = SENT_TO_PRINT_PROVIDER,
                                eventDateTime = batchResponse1.timestamp.toInstant().minusSeconds(10)
                            )
                        )
                    )
                )
            )
            val certificate2 = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        batchId = batchId2,
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
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
            val printRequest1 = certificate1.printRequests[0]
            assertThat(printRequest1.requestId).isEqualTo(newRequestId)
            assertThat(printRequest1.batchId).isNull()
            assertThat(printRequest1.statusHistory.sortedByDescending { it.eventDateTime }.first())
                .usingRecursiveComparison().isEqualTo(
                    PrintRequestStatus(
                        status = PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = batchResponse1.timestamp.toInstant(),
                        message = batchResponse1.message
                    )
                )
            assertThat(certificate2.printRequests[0].statusHistory.sortedByDescending { it.eventDateTime }.first())
                .usingRecursiveComparison().isEqualTo(
                    PrintRequestStatus(
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
        fun `should update print request given valid statusStep and status`() {
            // Given
            val requestId = aValidRequestId()
            val response = buildProcessPrintResponseMessage(requestId = requestId)
            val expectedStatus = IN_PRODUCTION
            given(statusMapper.toStatusEntityEnum(any(), any())).willReturn(expectedStatus)
            val certificate = buildCertificate(
                printRequests = listOf(
                    buildPrintRequest(
                        requestId = requestId,
                        printRequestStatuses = listOf(
                            buildPrintRequestStatus(
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
            verify(certificateRepository).getByPrintRequestsRequestId(requestId)
            verify(certificateRepository).save(certificate)
            assertThat(certificate.status).isEqualTo(expectedStatus)
            assertThat(certificate.printRequests[0].statusHistory.sortedByDescending { it.eventDateTime }.first())
                .usingRecursiveComparison().isEqualTo(
                    PrintRequestStatus(
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
            given(statusMapper.toStatusEntityEnum(any(), any())).willReturn(IN_PRODUCTION)
            given(certificateRepository.getByPrintRequestsRequestId(any())).willReturn(null)
            TestLogAppender.reset()

            // When
            service.processPrintResponse(response)

            // Then
            assertThat(TestLogAppender.hasLog("Certificate not found for the requestId $requestId", Level.ERROR)).isTrue
            verify(statusMapper).toStatusEntityEnum(response.statusStep, response.status)
            verify(certificateRepository).getByPrintRequestsRequestId(requestId)
            verify(certificateRepository, never()).save(any())
        }
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
        verifyNoInteractions(certificateRepository)
    }
}
