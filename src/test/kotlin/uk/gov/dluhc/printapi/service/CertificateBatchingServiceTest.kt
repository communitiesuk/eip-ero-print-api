package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.capture
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.given
import org.mockito.kotlin.secondValue
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.data.domain.Pageable
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH
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
    private lateinit var savedCertificatesArgumentCaptor: ArgumentCaptor<List<Certificate>>

    private val dailyLimit = 10

    private val maxUnBatchedRecords = 100

    private val fixedClock = Clock.fixed(Instant.parse("2022-11-25T23:59:59.999Z"), ZoneId.of("UTC"))

    @Test
    fun `should batch multiple print requests and save each`() {
        // Given
        val batchSize = 4
        certificateBatchingService = CertificateBatchingService(
            idFactory = idFactory,
            certificateRepository = certificateRepository,
            dailyLimit = dailyLimit,
            maxUnBatchedRecords = maxUnBatchedRecords,
            batchSize = batchSize,
            clock = fixedClock
        )

        val startOfDay = Instant.parse("2022-11-25T00:00:00.000Z")
        val endOfDay = Instant.parse("2022-11-25T23:59:59.000Z")

        val numberOfRequestsPendingAssignmentToBatch = 12
        val numberOfRequestsAlreadyAssignedToBatch = 3
        val certificates = (1..numberOfRequestsPendingAssignmentToBatch)
            .map { buildCertificate(status = PENDING_ASSIGNMENT_TO_BATCH) }
        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()

        given(certificateRepository.findByStatusOrderByApplicationReceivedDateTimeAsc(any(), any())).willReturn(
            certificates
        )
        given(certificateRepository.getPrintRequestStatusCount(any(), any(), any()))
            .willReturn(numberOfRequestsAlreadyAssignedToBatch)
        given(idFactory.batchId()).willReturn(batchId1, batchId2)

        // When
        val batchIds = certificateBatchingService.batchPendingCertificates()

        // Then
        verify(certificateRepository).findByStatusOrderByApplicationReceivedDateTimeAsc(
            PENDING_ASSIGNMENT_TO_BATCH,
            Pageable.ofSize(maxUnBatchedRecords)
        )
        verify(certificateRepository).getPrintRequestStatusCount(startOfDay, endOfDay, ASSIGNED_TO_BATCH)
        verify(idFactory, times(2)).batchId()
        verify(certificateRepository, times(2)).saveAll(capture(savedCertificatesArgumentCaptor))
        val firstBatchOfCertificates = savedCertificatesArgumentCaptor.firstValue
        assertThat(firstBatchOfCertificates.size).isEqualTo(4)
        val secondBatchOfCertificates = savedCertificatesArgumentCaptor.secondValue
        assertThat(secondBatchOfCertificates.size).isEqualTo(3)
        assertThat(batchIds).containsExactly(batchId1, batchId2)
    }

    @Test
    fun `should batch one print request and save`() {
        // Given
        val batchSize = 5
        certificateBatchingService = CertificateBatchingService(
            idFactory = idFactory,
            certificateRepository = certificateRepository,
            dailyLimit = dailyLimit,
            maxUnBatchedRecords = maxUnBatchedRecords,
            batchSize = batchSize,
            clock = fixedClock
        )

        val printRequests = listOf(
            buildPrintRequest(
                printRequestStatuses = listOf(buildPrintRequestStatus(status = PENDING_ASSIGNMENT_TO_BATCH)),
                batchId = null
            )
        )
        val certificates = listOf(buildCertificate(printRequests = printRequests))
        given(certificateRepository.findByStatusOrderByApplicationReceivedDateTimeAsc(any(), any()))
            .willReturn(certificates)

        val batchId = aValidBatchId()
        given(idFactory.batchId()).willReturn(batchId)

        // When
        val batchIds = certificateBatchingService.batchPendingCertificates()

        // Then
        verify(certificateRepository).findByStatusOrderByApplicationReceivedDateTimeAsc(
            PENDING_ASSIGNMENT_TO_BATCH,
            Pageable.ofSize(maxUnBatchedRecords)
        )
        verify(idFactory).batchId()
        verify(certificateRepository).saveAll(capture(savedCertificatesArgumentCaptor))
        assertThat(batchIds).containsExactly(batchId)
        val savedCertificates = savedCertificatesArgumentCaptor.value!!
        assertThat(savedCertificates.size).isEqualTo(1)
        assertThat(
            savedCertificates[0].printRequests[0].statusHistory.sortedBy { it.eventDateTime }
                .map { it.status }
        ).containsExactly(PENDING_ASSIGNMENT_TO_BATCH, ASSIGNED_TO_BATCH)
        assertThat(savedCertificates[0].status).isEqualTo(ASSIGNED_TO_BATCH)
    }

    @Test
    fun `should correctly batch print requests`() {
        // Given
        val batchSize = 3
        certificateBatchingService = CertificateBatchingService(
            idFactory = idFactory,
            certificateRepository = certificateRepository,
            dailyLimit = dailyLimit,
            maxUnBatchedRecords = maxUnBatchedRecords,
            batchSize = batchSize,
            clock = fixedClock
        )

        val numOfRequests = 8
        val certificates = (1..numOfRequests).map { buildCertificate(status = PENDING_ASSIGNMENT_TO_BATCH) }
        given(certificateRepository.findByStatusOrderByApplicationReceivedDateTimeAsc(any(), any()))
            .willReturn(certificates)
        given(idFactory.batchId()).willReturn(aValidBatchId(), aValidBatchId(), aValidBatchId())

        // When
        val batches = certificateBatchingService.batchCertificates()

        // Then
        assertThat(batches).hasSize(3)
        batches.map { (id, items) ->
            assert(items.all { it.status == ASSIGNED_TO_BATCH })
            assert(items.all { it.printRequests[0].batchId == id })
        }
        verify(certificateRepository).findByStatusOrderByApplicationReceivedDateTimeAsc(
            PENDING_ASSIGNMENT_TO_BATCH,
            Pageable.ofSize(maxUnBatchedRecords)
        )
        verify(idFactory, times(3)).batchId()
    }
}
