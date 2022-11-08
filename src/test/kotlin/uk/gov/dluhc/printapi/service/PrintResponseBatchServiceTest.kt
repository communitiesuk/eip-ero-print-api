package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.RECEIVED_BY_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.entity.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.FAILED
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse.Status.SUCCESS
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import java.time.Clock
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@ExtendWith(MockitoExtension::class)
class PrintResponseBatchServiceTest {
    private lateinit var service: PrintResponseBatchService

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
        service = PrintResponseBatchService(printDetailsRepository, idFactory, fixedClock)
    }

    @Test
    fun `should update print details given failed batch responses`() {
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
