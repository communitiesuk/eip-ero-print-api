package uk.gov.dluhc.printapi.service

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.dluhc.printapi.config.DataRetentionConfiguration
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.database.repository.DeliveryRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.printapi.messaging.models.SourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificateRemovalSummary
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildApplicationRemovedMessage
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.LocalDate
import java.time.Month.JANUARY
import java.time.Month.JULY
import java.util.UUID

@ExtendWith(MockitoExtension::class)
internal class CertificateDataRetentionServiceTest {

    @Mock
    private lateinit var certificateRemovalDateResolver: CertificateRemovalDateResolver

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Mock
    private lateinit var deliveryRepository: DeliveryRepository

    @Mock
    private lateinit var s3CertificatePhotoService: S3PhotoService

    @Mock
    private lateinit var removeCertificateQueue: MessageQueue<RemoveCertificateMessage>

    @Mock
    private lateinit var dataRetentionConfiguration: DataRetentionConfiguration

    @InjectMocks
    private lateinit var certificateDataRetentionService: CertificateDataRetentionService

    @Nested
    inner class HandleSourceApplicationRemoved {
        @Test
        fun `should handle source application removed`() {
            // Given
            val message = buildApplicationRemovedMessage()
            val certificate = buildCertificate()
            val expectedInitialRetentionRemovalDate = LocalDate.of(2023, JANUARY, 1)
            val expectedFinalRetentionRemovalDate = LocalDate.of(2032, JULY, 1)
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(certificate)
            given(certificateRemovalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(any(), any())).willReturn(expectedInitialRetentionRemovalDate)
            given(certificateRemovalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(any())).willReturn(expectedFinalRetentionRemovalDate)

            // When
            certificateDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            assertThat(certificate).hasInitialRetentionRemovalDate(expectedInitialRetentionRemovalDate)
            assertThat(certificate).hasFinalRetentionRemovalDate(expectedFinalRetentionRemovalDate)
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(certificateRepository).findByGssCodeAndSourceTypeAndSourceReference(message.gssCode, VOTER_CARD, message.sourceReference)
            verify(certificateRemovalDateResolver).getCertificateInitialRetentionPeriodRemovalDate(certificate.issueDate, message.gssCode)
            verify(certificateRemovalDateResolver).getElectorDocumentFinalRetentionPeriodRemovalDate(certificate.issueDate)
            verify(certificateRepository).save(certificate)
        }

        @Test
        fun `should log error when certificate doesn't exist`() {
            // Given
            val message = buildApplicationRemovedMessage(
                sourceReference = "63774ff4bb4e7049b67182d9",
                sourceType = VOTER_MINUS_CARD
            )
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(null)
            TestLogAppender.reset()

            // When
            certificateDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(certificateRepository).findByGssCodeAndSourceTypeAndSourceReference(message.gssCode, VOTER_CARD, message.sourceReference)
            verify(certificateRepository, times(0)).save(any())
            verifyNoInteractions(certificateRemovalDateResolver)
            assertThat(
                TestLogAppender.hasLog(
                    "Certificate with sourceType = VOTER_CARD and sourceReference = 63774ff4bb4e7049b67182d9 not found",
                    Level.ERROR
                )
            ).isTrue
        }
    }

    @Nested
    inner class RemoveInitialRetentionPeriodData {
        @Test
        fun `should remove initial retention period data`() {
            // Given
            val certificate1 = buildCertificate()
            val certificate2 = buildCertificate()
            val delivery1 = certificate1.printRequests[0].delivery!!
            val delivery2 = certificate2.printRequests[0].delivery!!
            given(certificateRepository.findPendingRemovalOfInitialRetentionData(VOTER_CARD)).willReturn(listOf(certificate1, certificate2))

            // When
            certificateDataRetentionService.removeInitialRetentionPeriodData(VOTER_CARD)

            // Then
            assertThat(certificate1).initialRetentionPeriodDataIsRemoved()
            assertThat(certificate2).initialRetentionPeriodDataIsRemoved()
            verify(certificateRepository).findPendingRemovalOfInitialRetentionData(VOTER_CARD)
            verify(deliveryRepository).delete(delivery1)
            verify(deliveryRepository).delete(delivery2)
        }

        @Test
        fun `should not remove initial retention period data given no certificates found`() {
            // Given
            val certificate = buildCertificate()
            given(certificateRepository.findPendingRemovalOfInitialRetentionData(VOTER_CARD)).willReturn(
                emptyList()
            )

            // When
            certificateDataRetentionService.removeInitialRetentionPeriodData(VOTER_CARD)

            // Then
            assertThat(certificate).hasInitialRetentionPeriodData()
            verify(certificateRepository).findPendingRemovalOfInitialRetentionData(VOTER_CARD)
            verifyNoInteractions(deliveryRepository)
        }
    }

    @Nested
    inner class QueueCertificatesForRemoval {
        @Test
        fun `should queue certificates for removal`() {
            // Given
            val certificateRemovalSummary1 = buildCertificateRemovalSummary()
            val certificateRemovalSummary2 = buildCertificateRemovalSummary()
            val summaries = listOf(certificateRemovalSummary1, certificateRemovalSummary2)
            val batchSize = 10000
            given(dataRetentionConfiguration.certificateRemovalBatchSize).willReturn(batchSize)
            given(certificateRepository.countBySourceTypeAndFinalRetentionRemovalDateBefore(VOTER_CARD)).willReturn(2)
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD, 1, batchSize)).willReturn(PageImpl(summaries))
            TestLogAppender.reset()

            // When
            certificateDataRetentionService.queueCertificatesForRemoval(VOTER_CARD)

            // Then
            verify(certificateRepository).countBySourceTypeAndFinalRetentionRemovalDateBefore(VOTER_CARD)
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD, 1, batchSize)
            verify(removeCertificateQueue).submit(RemoveCertificateMessage(certificateRemovalSummary1.id!!, certificateRemovalSummary1.photoLocationArn!!))
            verify(removeCertificateQueue).submit(RemoveCertificateMessage(certificateRemovalSummary2.id!!, certificateRemovalSummary2.photoLocationArn!!))
            assertThat(TestLogAppender.hasLog("Found 2 certificates with sourceType VOTER_CARD to remove", Level.INFO)).isTrue
        }

        @Test
        fun `should retrieve certificates in batches and queue for removal`() {
            // Given
            val batchSize = 4
            val removalSummaries = listOf(buildCertificateRemovalSummary(), buildCertificateRemovalSummary(), buildCertificateRemovalSummary())
            given(dataRetentionConfiguration.certificateRemovalBatchSize).willReturn(batchSize)
            // initial query to retrieve the number of results
            given(certificateRepository.countBySourceTypeAndFinalRetentionRemovalDateBefore(any(), any())).willReturn(13)

            // the batched calls in the exact expected order
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD, 4, batchSize))
                .willReturn(PageImpl(removalSummaries, PageRequest.of(4, batchSize), 13))
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD, 3, batchSize))
                .willReturn(PageImpl(removalSummaries, PageRequest.of(3, batchSize), 13))
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD, 2, batchSize))
                .willReturn(PageImpl(removalSummaries, PageRequest.of(2, batchSize), 13))
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD, 1, batchSize))
                .willReturn(PageImpl(removalSummaries, PageRequest.of(1, batchSize), 13))
            TestLogAppender.reset()

            // When
            certificateDataRetentionService.queueCertificatesForRemoval(VOTER_CARD)

            // Then
            verify(certificateRepository).countBySourceTypeAndFinalRetentionRemovalDateBefore(VOTER_CARD)
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD, 4, batchSize)
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD, 3, batchSize)
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD, 2, batchSize)
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD, 1, batchSize)
            assertThat(TestLogAppender.hasLog("Found 13 certificates with sourceType VOTER_CARD to remove", Level.INFO)).isTrue
            assertThat(TestLogAppender.hasLog("Retrieving batch [4] of [4] of CertificateRemovalSummary", Level.INFO)).isTrue
            assertThat(TestLogAppender.hasLog("Retrieving batch [3] of [4] of CertificateRemovalSummary", Level.INFO)).isTrue
            assertThat(TestLogAppender.hasLog("Retrieving batch [2] of [4] of CertificateRemovalSummary", Level.INFO)).isTrue
            assertThat(TestLogAppender.hasLog("Retrieving batch [1] of [4] of CertificateRemovalSummary", Level.INFO)).isTrue
        }

        @Test
        fun `should not queue certificates for removal given no certificates due to be removed`() {
            // Given
            given(certificateRepository.countBySourceTypeAndFinalRetentionRemovalDateBefore(VOTER_CARD)).willReturn(0)
            TestLogAppender.reset()

            // When
            certificateDataRetentionService.queueCertificatesForRemoval(VOTER_CARD)

            // Then
            verify(certificateRepository).countBySourceTypeAndFinalRetentionRemovalDateBefore(VOTER_CARD)
            verifyNoMoreInteractions(certificateRepository)
            assertThat(TestLogAppender.hasLog("No certificates with sourceType VOTER_CARD to remove final retention period data from", Level.INFO)).isTrue
        }
    }

    @Nested
    inner class RemoveFinalRetentionPeriodData {

        @Test
        fun `should remove certificate final retention data`() {
            // Given
            val message = RemoveCertificateMessage(certificateId = UUID.randomUUID(), certificatePhotoArn = aPhotoArn())

            // When
            certificateDataRetentionService.removeFinalRetentionPeriodData(message)

            // Then
            verify(s3CertificatePhotoService).removePhoto(message.certificatePhotoArn)
            verify(certificateRepository).deleteById(message.certificateId)
        }
    }
}
