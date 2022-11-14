package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.rds.certificateBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.rds.printRequestStatusBuilder

@ExtendWith(MockitoExtension::class)
class PrintRequestsServiceTest {

    @InjectMocks
    private lateinit var printRequestsService: PrintRequestsService

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Mock
    private lateinit var printDetailsRepository: PrintDetailsRepository

    @Mock
    private lateinit var processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>

    @Captor
    private lateinit var savedRecordArgumentCaptor: ArgumentCaptor<PrintDetails>

    @Captor
    private lateinit var savedCertificateArgumentCaptor: ArgumentCaptor<Certificate>

    @Test
    fun `should batch multiple print requests, save and submit to queue`() {
        // Given
        val batchSize = 5
        val numOfRequests = 12
        val items = (1..numOfRequests).map { buildPrintDetails() }
        val certificates = (1..numOfRequests).map { certificateBuilder(status = PENDING_ASSIGNMENT_TO_BATCH) }

        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()
        val batchId3 = aValidBatchId()
        val batchId4 = aValidBatchId()
        val batchId5 = aValidBatchId()
        val batchId6 = aValidBatchId()

        given(printDetailsRepository.getAllByStatus(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(items)
        given(certificateRepository.findByStatusIs(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)
        given(idFactory.batchId()).willReturn(batchId1, batchId2, batchId3, batchId4, batchId5, batchId6)

        // When
        printRequestsService.processPrintRequests(batchSize)

        // Then
        verify(printDetailsRepository).getAllByStatus(PENDING_ASSIGNMENT_TO_BATCH)
        verify(certificateRepository).findByStatusIs(PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory, times(3 * 2)).batchId()
        verify(printDetailsRepository, times(12)).save(any())
        verify(certificateRepository, times(12)).save(any())
        verify(processPrintRequestQueue, times(3 * 2)).submit(any())
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId1))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId2))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId3))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId4))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId5))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId6))
    }

    @Test
    fun `should batch one print request, save and submit to queue`() {
        // Given
        val batchSize = 5

        val aPrintDetailsRecord = buildPrintDetails(
            batchId = null,
            status = PENDING_ASSIGNMENT_TO_BATCH
        )
        val items = listOf(aPrintDetailsRecord)
        given(printDetailsRepository.getAllByStatus(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(items)

        val printRequests = listOf(
            printRequestBuilder(
                printRequestStatuses = listOf(printRequestStatusBuilder(status = PENDING_ASSIGNMENT_TO_BATCH)),
                batchId = null
            )
        )
        val certificates = listOf(certificateBuilder(printRequests = printRequests))
        given(certificateRepository.findByStatusIs(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)

        val batchId = aValidBatchId()
        given(idFactory.batchId()).willReturn(batchId)
        val expectedMessage = ProcessPrintRequestBatchMessage(batchId)

        // When
        printRequestsService.processPrintRequests(batchSize)

        // Then
        verify(printDetailsRepository).getAllByStatus(PENDING_ASSIGNMENT_TO_BATCH)
        verify(certificateRepository).findByStatusIs(PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory, times(2)).batchId()
        verify(processPrintRequestQueue, times(2)).submit(expectedMessage)

        verify(printDetailsRepository).save(capture(savedRecordArgumentCaptor))
        val savedPrintDetails = savedRecordArgumentCaptor.value!!
        assertThat(savedPrintDetails.printRequestStatuses!!.sortedBy { it.eventDateTime }.map { it.status })
            .containsExactly(PENDING_ASSIGNMENT_TO_BATCH, Status.ASSIGNED_TO_BATCH)
        assertThat(savedPrintDetails.status).isEqualTo(Status.ASSIGNED_TO_BATCH)

        verify(certificateRepository).save(capture(savedCertificateArgumentCaptor))
        val savedCertificates = savedCertificateArgumentCaptor.value!!
        assertThat(savedCertificates.getCurrentPrintRequest().statusHistory.sortedBy { it.eventDateTime }.map { it.status })
            .containsExactly(PENDING_ASSIGNMENT_TO_BATCH, Status.ASSIGNED_TO_BATCH)
        assertThat(savedCertificates.status).isEqualTo(Status.ASSIGNED_TO_BATCH)
    }

    @Test
    fun `should correctly batch print requests`() {
        // Given
        val batchSize = 10
        val numOfRequests = 23
        val items = (1..numOfRequests).map { buildPrintDetails() }
        given(printDetailsRepository.getAllByStatus(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(items)
        given(idFactory.batchId()).willReturn(aValidBatchId(), aValidBatchId(), aValidBatchId())

        // When
        val batches = printRequestsService.batchPrintRequests(batchSize)

        // Then
        assertThat(batches).hasSize(3)
        batches.map { (id, items) ->
            assert(items.all { it.status == Status.ASSIGNED_TO_BATCH })
            assert(items.all { it.batchId == id })
        }
    }
}
