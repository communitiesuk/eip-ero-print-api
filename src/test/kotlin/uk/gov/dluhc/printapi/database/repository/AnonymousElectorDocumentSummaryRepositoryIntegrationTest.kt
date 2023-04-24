package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.anotherGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryViewFromAedEntity
import java.time.LocalDate

internal class AnonymousElectorDocumentSummaryRepositoryIntegrationTest : IntegrationTest() {

    @Nested
    inner class FindByGssCodeInAndSourceTypeOrderByDateCreatedDescSanitizedSurnameAsc {

        @Test
        fun `should returns empty list when summary view has no records`() {
            // Given
            val gssCode = aGssCode()

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findByGssCodeInAndSourceTypeOrderByIssueDateDescSanitizedSurnameAsc(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should returns empty list when no matching gssCode record exists`() {
            // Given
            val gssCode = aGssCode()
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())
            anonymousElectorDocumentRepository.saveAll(listOf(otherAed))

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findByGssCodeInAndSourceTypeOrderByIssueDateDescSanitizedSurnameAsc(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should find AEDs for a given gssCode and source type in required ordering`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()

            val aed1 = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.minusDays(10),
                contactDetails = buildAedContactDetails(surname = "Z")
            )
            val aed2 = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.minusDays(10),
                contactDetails = buildAedContactDetails(surname = "A")
            )
            val aed3 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(10))
            val aed4 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2, aed3, aed4, otherAed))

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed3)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed4)
            val expectedSummaryRecord3 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed2)
            val expectedSummaryRecord4 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed1)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findByGssCodeInAndSourceTypeOrderByIssueDateDescSanitizedSurnameAsc(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT
                )

            // Then
            assertThat(actual)
                .doesNotContain(otherAedSummaryRecord)
                .containsExactlyInAnyOrder(
                    expectedSummaryRecord1,
                    expectedSummaryRecord2,
                    expectedSummaryRecord3,
                    expectedSummaryRecord4
                )
        }
    }
}
