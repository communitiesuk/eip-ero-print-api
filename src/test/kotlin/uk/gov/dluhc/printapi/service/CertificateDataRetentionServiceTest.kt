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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.CertificateRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.database.repository.DeliveryRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.SourceType.VOTER_MINUS_CARD
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildApplicationRemovedMessage
import java.time.LocalDate

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

    @InjectMocks
    private lateinit var certificateDataRetentionService: CertificateDataRetentionService

    @Nested
    inner class HandleSourceApplicationRemoved {
        @Test
        fun `should handle source application removed`() {
            // Given
            val message = buildApplicationRemovedMessage()
            val certificate = buildCertificate()
            val expectedInitialRetentionRemovalDate = LocalDate.of(2023, 1, 1)
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(certificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(
                certificate
            )
            given(
                certificateRemovalDateResolver.getCertificateInitialRetentionPeriodRemovalDate(
                    any(),
                    any()
                )
            ).willReturn(
                expectedInitialRetentionRemovalDate
            )

            // When
            certificateDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            assertThat(certificate.initialRetentionRemovalDate).isEqualTo(expectedInitialRetentionRemovalDate)
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(certificateRepository).findByGssCodeAndSourceTypeAndSourceReference(
                message.gssCode,
                VOTER_CARD,
                message.sourceReference
            )
            verify(certificateRepository).save(certificate)
        }

        @Test
        fun `should throw exception when certificate doesn't exist`() {
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
            verify(certificateRepository).findByGssCodeAndSourceTypeAndSourceReference(
                message.gssCode,
                VOTER_CARD,
                message.sourceReference
            )
            verify(certificateRepository, times(0)).save(any())
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
            given(certificateRepository.findPendingRemovalOfInitialRetentionData(VOTER_CARD)).willReturn(
                listOf(
                    certificate1,
                    certificate2
                )
            )

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
    inner class RemoveFinalRetentionPeriodData {
        @Test
        fun `should remove final retention period data`() {
            // Given
            val certificate1 = buildCertificate()
            val certificate2 = buildCertificate()
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD)).willReturn(
                listOf(
                    certificate1,
                    certificate2
                )
            )

            // When
            certificateDataRetentionService.removeFinalRetentionPeriodData(VOTER_CARD)

            // Then
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD)
            verify(certificateRepository).delete(certificate1)
            verify(certificateRepository).delete(certificate2)
        }

        @Test
        fun `should not remove final retention period data given no certificates found`() {
            // Given
            val certificate = buildCertificate()
            given(certificateRepository.findPendingRemovalOfFinalRetentionData(VOTER_CARD)).willReturn(
                emptyList()
            )

            // When
            certificateDataRetentionService.removeFinalRetentionPeriodData(VOTER_CARD)

            // Then
            verify(certificateRepository).findPendingRemovalOfFinalRetentionData(VOTER_CARD)
            verify(certificateRepository, never()).delete(certificate)
        }
    }
}
