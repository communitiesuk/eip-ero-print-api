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
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildApplicationRemovedMessage
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.anotherPhotoArn
import java.time.LocalDate
import java.time.Month

@ExtendWith(MockitoExtension::class)
internal class AedDataRetentionServiceTest {

    @Mock
    private lateinit var sourceTypeMapper: SourceTypeMapper

    @Mock
    private lateinit var anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository

    @Mock
    private lateinit var removalDateResolver: ElectorDocumentRemovalDateResolver

    @Mock
    private lateinit var s3PhotoService: S3PhotoService

    @InjectMocks
    private lateinit var aedDataRetentionService: AedDataRetentionService

    @Nested
    inner class HandleSourceApplicationRemoved {
        @Test
        fun `should handle source application removed`() {
            // Given
            val message = buildApplicationRemovedMessage()
            val issueDate = LocalDate.of(2023, Month.JANUARY, 1)
            val anonymousElectorDocument = buildAnonymousElectorDocument(issueDate = issueDate)
            val expectedFinalRetentionRemovalDate = LocalDate.of(2032, Month.JULY, 1)
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(anonymousElectorDocument)
            given(removalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(any())).willReturn(expectedFinalRetentionRemovalDate)

            // When
            aedDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            assertThat(anonymousElectorDocument.finalRetentionRemovalDate).isEqualTo(expectedFinalRetentionRemovalDate)
            assertThat(anonymousElectorDocument).hasFinalRetentionRemovalDate(expectedFinalRetentionRemovalDate)
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(anonymousElectorDocumentRepository).findByGssCodeAndSourceTypeAndSourceReference(message.gssCode, VOTER_CARD, message.sourceReference)
            verify(removalDateResolver).getElectorDocumentFinalRetentionPeriodRemovalDate(issueDate)
            verify(anonymousElectorDocumentRepository).save(anonymousElectorDocument)
        }

        @Test
        fun `should log error when temporary certificate doesn't exist`() {
            // Given
            val message = buildApplicationRemovedMessage(
                sourceReference = "63774ff4bb4e7049b67182d9"
            )
            given(sourceTypeMapper.mapSqsToEntity(any())).willReturn(VOTER_CARD)
            given(anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(any(), any(), any())).willReturn(null)
            TestLogAppender.reset()

            // When
            aedDataRetentionService.handleSourceApplicationRemoved(message)

            // Then
            verify(sourceTypeMapper).mapSqsToEntity(message.sourceType)
            verify(anonymousElectorDocumentRepository).findByGssCodeAndSourceTypeAndSourceReference(message.gssCode, VOTER_CARD, message.sourceReference)
            verify(anonymousElectorDocumentRepository, times(0)).save(any())
            verifyNoInteractions(removalDateResolver)
            assertThat(
                TestLogAppender.hasLog(
                    "Anonymous Elector Document with sourceType = VOTER_CARD and sourceReference = 63774ff4bb4e7049b67182d9 not found",
                    Level.ERROR
                )
            ).isTrue
        }
    }

    @Nested
    inner class RemoveFinalRetentionPeriodData {

        @Test
        fun `should remove anonymous elector document final retention data`() {
            // Given
            val aed1 = buildAnonymousElectorDocument(photoLocationArn = aPhotoArn())
            val aed2 = buildAnonymousElectorDocument(photoLocationArn = anotherPhotoArn())
            val sourceType = ANONYMOUS_ELECTOR_DOCUMENT
            given(anonymousElectorDocumentRepository.findPendingRemovalOfFinalRetentionData(sourceType)).willReturn(listOf(aed1, aed2))

            // When
            aedDataRetentionService.removeAnonymousElectorDocumentData(sourceType)

            // Then
            verify(anonymousElectorDocumentRepository).findPendingRemovalOfFinalRetentionData(sourceType)
            verify(s3PhotoService).removePhoto(aed1.photoLocationArn)
            verify(s3PhotoService).removePhoto(aed2.photoLocationArn)
            verify(anonymousElectorDocumentRepository).deleteById(aed1.id!!)
            verify(anonymousElectorDocumentRepository).deleteById(aed2.id!!)
        }
    }
}
