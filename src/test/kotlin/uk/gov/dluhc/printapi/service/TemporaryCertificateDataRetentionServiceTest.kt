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
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildApplicationRemovedMessage
import java.time.LocalDate
import java.time.Month.JANUARY
import java.time.Month.JULY

@ExtendWith(MockitoExtension::class)
internal class TemporaryCertificateDataRetentionServiceTest {

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var temporaryCertificateRepository: TemporaryCertificateRepository

    @InjectMocks
    private lateinit var temporaryCertificateDataRetentionService: TemporaryCertificateDataRetentionService

    @Nested
    inner class HandleSourceApplicationRemoved {
        @Test
        fun `should handle source application removed`() {
            // Given
            val message = buildApplicationRemovedMessage()
            val temporaryCertificate = buildTemporaryCertificate(issueDate = LocalDate.of(2023, JANUARY, 1))
            val expectedFinalRetentionRemovalDate = LocalDate.of(2024, JULY, 1)
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(temporaryCertificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(temporaryCertificate)

            // When
            temporaryCertificateDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            assertThat(temporaryCertificate.finalRetentionRemovalDate).isEqualTo(expectedFinalRetentionRemovalDate)
            assertThat(temporaryCertificate).hasFinalRetentionRemovalDate(expectedFinalRetentionRemovalDate)
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(temporaryCertificateRepository).findByGssCodeAndSourceTypeAndSourceReference(message.gssCode, VOTER_CARD, message.sourceReference)
            verify(temporaryCertificateRepository).save(temporaryCertificate)
        }

        @Test
        fun `should log error when temporary certificate doesn't exist`() {
            // Given
            val message = buildApplicationRemovedMessage(
                sourceReference = "63774ff4bb4e7049b67182d9"
            )
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(temporaryCertificateRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(null)
            TestLogAppender.reset()

            // When
            temporaryCertificateDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(temporaryCertificateRepository).findByGssCodeAndSourceTypeAndSourceReference(message.gssCode, VOTER_CARD, message.sourceReference)
            verify(temporaryCertificateRepository, times(0)).save(any())
            assertThat(
                TestLogAppender.hasLog(
                    "Temporary certificate with sourceType = VOTER_CARD and sourceReference = 63774ff4bb4e7049b67182d9 not found",
                    Level.ERROR
                )
            ).isTrue
        }
    }
}
