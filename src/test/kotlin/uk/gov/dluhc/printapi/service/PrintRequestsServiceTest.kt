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
        val batch1 = Pair(aValidBatchId(), 6)
        val batch2 = Pair(aValidBatchId(), 23)
        val batch3 = Pair(aValidBatchId(), 2)
        val batchIds = listOf(batch1, batch2, batch3)
        given(certificateBatchingService.batchPendingCertificates()).willReturn(batchIds)

        // When
        printRequestsService.processPrintRequests()

        // Then
        verify(certificateBatchingService).batchPendingCertificates()
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batch1.first, batch1.second))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batch2.first, batch2.second))
        verify(processPrintRequestQueue).submit(ProcessPrintRequestBatchMessage(batch3.first, batch3.second))
        verifyNoMoreInteractions(processPrintRequestQueue)
    }
}
