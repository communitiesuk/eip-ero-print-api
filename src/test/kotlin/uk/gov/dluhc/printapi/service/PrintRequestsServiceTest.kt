package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintRequestBatchMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId

@ExtendWith(MockitoExtension::class)
class PrintRequestsServiceTest {

    @InjectMocks
    private lateinit var printRequestsService: PrintRequestsService

    @Mock
    private lateinit var certificateBatchingService: CertificateBatchingService

    @Mock
    private lateinit var processPrintRequestQueue: MessageQueue<ProcessPrintRequestBatchMessage>

    @Test
    fun `should process print requests and submit to queue`() {
        // Given
        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()
        val batchId3 = aValidBatchId()
        val batchIds = setOf(batchId1, batchId2, batchId3)
        given(certificateBatchingService.batchPendingCertificates()).willReturn(batchIds)

        // When
        printRequestsService.processPrintRequests()

        // Then
        verify(certificateBatchingService).batchPendingCertificates()
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId1))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId2))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batchId3))
        verifyNoMoreInteractions(processPrintRequestQueue)
    }
}
