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
import uk.gov.dluhc.printapi.database.entity.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.Status.PENDING_ASSIGNMENT_TO_BATCH
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
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
    private lateinit var processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>

    @Captor
    private lateinit var savedCertificateArgumentCaptor: ArgumentCaptor<Certificate>

    @Test
    fun `should batch multiple print requests, save and submit to queue`() {
        // Given
        val batchSize = 5
        val numOfRequests = 12
        val certificates = (1..numOfRequests).map { certificateBuilder(status = PENDING_ASSIGNMENT_TO_BATCH) }

        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()
        val batchId3 = aValidBatchId()
        given(certificateRepository.findByStatus(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)
        given(idFactory.batchId()).willReturn(batchId1, batchId2, batchId3)

        // When
        printRequestsService.processPrintRequests(batchSize)

        // Then
        verify(certificateRepository).findByStatus(PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory, times(3)).batchId()
        verify(certificateRepository, times(12)).save(any())
        verify(processPrintRequestQueue, times(3)).submit(any())
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId1))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId2))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId3))
    }

    @Test
    fun `should batch one print request, save and submit to queue`() {
        // Given
        val batchSize = 5

        val printRequests = listOf(
            printRequestBuilder(
                printRequestStatuses = listOf(printRequestStatusBuilder(status = PENDING_ASSIGNMENT_TO_BATCH)),
                batchId = null
            )
        )
        val certificates = listOf(certificateBuilder(printRequests = printRequests))
        given(certificateRepository.findByStatus(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)

        val batchId = aValidBatchId()
        given(idFactory.batchId()).willReturn(batchId)
        val expectedMessage = ProcessPrintRequestBatchMessage(batchId)

        // When
        printRequestsService.processPrintRequests(batchSize)

        // Then
        verify(certificateRepository).findByStatus(PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory).batchId()
        verify(processPrintRequestQueue).submit(expectedMessage)

        verify(certificateRepository).save(capture(savedCertificateArgumentCaptor))
        val savedCertificates = savedCertificateArgumentCaptor.value!!
        assertThat(savedCertificates.getCurrentPrintRequest().statusHistory.sortedBy { it.eventDateTime }.map { it.status })
            .containsExactly(PENDING_ASSIGNMENT_TO_BATCH, ASSIGNED_TO_BATCH)
        assertThat(savedCertificates.status).isEqualTo(ASSIGNED_TO_BATCH)
    }

    @Test
    fun `should correctly batch print requests`() {
        // Given
        val batchSize = 10
        val numOfRequests = 23
        val certificates = (1..numOfRequests).map { certificateBuilder() }
        given(certificateRepository.findByStatus(PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)
        given(idFactory.batchId()).willReturn(aValidBatchId(), aValidBatchId(), aValidBatchId())

        // When
        val batches = printRequestsService.batchCertificates(batchSize)

        // Then
        assertThat(batches).hasSize(3)
        batches.map { (id, items) ->
            assert(items.all { it.status == ASSIGNED_TO_BATCH })
            assert(items.all { it.getCurrentPrintRequest().batchId == id })
        }
    }
}
