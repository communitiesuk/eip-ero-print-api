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
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintStatus

@ExtendWith(MockitoExtension::class)
internal class CertificateBatchingServiceTest {

    @InjectMocks
    private lateinit var certificateBatchingService: CertificateBatchingService

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Captor
    private lateinit var savedCertificateArgumentCaptor: ArgumentCaptor<Certificate>

    @Test
    fun `should batch multiple print requests and save each`() {
        // Given
        val batchSize = 5
        val numOfRequests = 12
        val certificates = (1..numOfRequests).map { buildCertificate(status = Status.PENDING_ASSIGNMENT_TO_BATCH) }

        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()
        val batchId3 = aValidBatchId()
        given(certificateRepository.findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)
        given(idFactory.batchId()).willReturn(batchId1, batchId2, batchId3)

        // When
        val batchIds = certificateBatchingService.batchPendingCertificates(batchSize)

        // Then
        verify(certificateRepository).findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory, times(3)).batchId()
        verify(certificateRepository, times(12)).save(any())
        assertThat(batchIds).containsExactly(batchId1, batchId2, batchId3)
    }

    @Test
    fun `should batch one print request and save`() {
        // Given
        val batchSize = 5

        val printRequests = listOf(
            buildPrintRequest(
                printRequestStatuses = listOf(buildPrintStatus(status = Status.PENDING_ASSIGNMENT_TO_BATCH)),
                batchId = null
            )
        )
        val certificates = listOf(buildCertificate(printRequests = printRequests))
        given(certificateRepository.findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)

        val batchId = aValidBatchId()
        given(idFactory.batchId()).willReturn(batchId)

        // When
        val batchIds = certificateBatchingService.batchPendingCertificates(batchSize)

        // Then
        verify(certificateRepository).findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        verify(idFactory).batchId()
        verify(certificateRepository).save(capture(savedCertificateArgumentCaptor))
        assertThat(batchIds).containsExactly(batchId)
        val savedCertificates = savedCertificateArgumentCaptor.value!!
        assertThat(
            savedCertificates.getCurrentPrintRequest().statusHistory.sortedBy { it.eventDateTime }
                .map { it.status }
        )
            .containsExactly(Status.PENDING_ASSIGNMENT_TO_BATCH, Status.ASSIGNED_TO_BATCH)

        assertThat(savedCertificates.status).isEqualTo(Status.ASSIGNED_TO_BATCH)
    }

    @Test
    fun `should correctly batch print requests`() {
        // Given
        val batchSize = 10
        val numOfRequests = 23
        val certificates = (1..numOfRequests).map { buildCertificate() }
        given(certificateRepository.findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)
        given(idFactory.batchId()).willReturn(aValidBatchId(), aValidBatchId(), aValidBatchId())

        // When
        val batches = certificateBatchingService.batchCertificates(batchSize)

        // Then
        assertThat(batches).hasSize(3)
        batches.map { (id, items) ->
            assert(items.all { it.status == Status.ASSIGNED_TO_BATCH })
            assert(items.all { it.getCurrentPrintRequest().batchId == id })
        }
    }
}