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
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@ExtendWith(MockitoExtension::class)
internal class CertificateBatchingServiceTest {

    private lateinit var certificateBatchingService: CertificateBatchingService

    @Mock
    private lateinit var idFactory: IdFactory

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Captor
    private lateinit var savedCertificateArgumentCaptor: ArgumentCaptor<Certificate>

    private val dailyLimit: Int = 10

    private val fixedClock = Clock.fixed(Instant.parse("2022-11-25T23:59:59.999Z"), ZoneId.of("UTC"))

    @BeforeEach
    fun setup() {
        certificateBatchingService = CertificateBatchingService(
            idFactory = idFactory,
            certificateRepository = certificateRepository,
            dailyLimit = dailyLimit,
            clock = fixedClock
        )
    }

    @Test
    fun `should batch multiple print requests and save each`() {
        // Given
        val startOfDay = Instant.parse("2022-11-25T00:00:00.000Z")
        val endOfDay = Instant.parse("2022-11-25T23:59:59.000Z")

        val batchSize = 4
        val numberOfRequestsPendingAssignmentToBatch = 12
        val numberOfRequestsAlreadyAssignedToBatch = 3
        val certificates = (1..numberOfRequestsPendingAssignmentToBatch)
            .map { buildCertificate(status = Status.PENDING_ASSIGNMENT_TO_BATCH) }
        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()

        given(certificateRepository.findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)).willReturn(certificates)
        given(certificateRepository.getPrintRequestStatusCount(any(), any(), any()))
            .willReturn(numberOfRequestsAlreadyAssignedToBatch)
        given(idFactory.batchId()).willReturn(batchId1, batchId2)

        // When
        val batchIds = certificateBatchingService.batchPendingCertificates(batchSize)

        // Then
        verify(certificateRepository).findByStatus(Status.PENDING_ASSIGNMENT_TO_BATCH)
        verify(certificateRepository).getPrintRequestStatusCount(startOfDay, endOfDay, Status.ASSIGNED_TO_BATCH)
        verify(idFactory, times(2)).batchId()
        verify(certificateRepository, times(7)).save(any())
        assertThat(batchIds).containsExactly(batchId1, batchId2)
    }

    @Test
    fun `should batch one print request and save`() {
        // Given
        val batchSize = 5

        val printRequests = listOf(
            buildPrintRequest(
                printRequestStatuses = listOf(buildPrintRequestStatus(status = Status.PENDING_ASSIGNMENT_TO_BATCH)),
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
        val batchSize = 3
        val numOfRequests = 8
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
