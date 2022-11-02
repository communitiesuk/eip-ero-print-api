package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails

@ExtendWith(MockitoExtension::class)
class PrintRequestsServiceTest {
    @InjectMocks
    private lateinit var printRequestsService: PrintRequestsService

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var printDetailsRepository: PrintDetailsRepository

    @Mock
    private lateinit var processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>

    @Test
    fun `should batch print requests, save and submit to queue`() {
        // Given
        val batchSize = 5
        val numOfRequests = 12
        val items = (1..numOfRequests).map { buildPrintDetails() }

        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()
        val batchId3 = aValidBatchId()

        given(printDetailsRepository.getAllByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)).willReturn(items)
        given(idFactory.batchId()).willReturn(batchId1, batchId2, batchId3)

        // When
        printRequestsService.processPrintRequests(batchSize)

        // Then
        verify(printDetailsRepository).getAllByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory, times(3)).batchId()
        verify(printDetailsRepository, times(12)).save(any())
        verify(processPrintRequestQueue, times(3)).submit(any())
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId1))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId2))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId3))
    }

    @Test
    fun `should correctly batch print requests`() {
        // Given
        val batchSize = 10
        val numOfRequests = 23
        val items = (1..numOfRequests).map { buildPrintDetails() }
        given(printDetailsRepository.getAllByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)).willReturn(items)
        given(idFactory.batchId()).willReturn(aValidBatchId(), aValidBatchId(), aValidBatchId())

        // When
        val batches = printRequestsService.batchPrintRequests(batchSize)

        // Then
        assert(batches.size == 3)
        batches.map { (id, items) ->
            assert(items.all { it.status == Status.PENDING_ASSIGNMENT_TO_BATCH })
            assert(items.all { it.batchId == id })
        }
    }
}
