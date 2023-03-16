package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import java.time.LocalDate

internal class AnonymousElectorDocumentRepositoryIntegrationTest : IntegrationTest() {

    @Nested
    inner class GetAnonymousElectorDocumentsForInitialRetentionDataRemoval {
        @Test
        fun `should find AEDs for removal of initial retention period data`() {
            // Given
            val expected1 = buildAnonymousElectorDocument(
                initialRetentionRemovalDate = LocalDate.now().minusDays(1)
            )
            val expected2 = buildAnonymousElectorDocument(
                initialRetentionRemovalDate = LocalDate.now().minusDays(1)
            )

            val other1 = buildAnonymousElectorDocument(
                initialRetentionRemovalDate = LocalDate.now().minusDays(1),
                initialRetentionDataRemoved = true // should be excluded
            )

            val other2 = buildAnonymousElectorDocument(
                initialRetentionRemovalDate = LocalDate.now().plusDays(1)
            )

            anonymousElectorDocumentRepository.saveAll(listOf(other1, expected1, other2, expected2))

            // When
            val actual = anonymousElectorDocumentRepository.findPendingRemovalOfInitialRetentionData(ANONYMOUS_ELECTOR_DOCUMENT)

            // Then
            assertThat(actual).containsExactlyInAnyOrder(expected1, expected2)
        }
    }

    @Nested
    inner class GetAnonymousElectorDocumentsForFinalRetentionDataRemoval {
        @Test
        fun `should find AEDs for removal of final retention period data`() {
            // Given
            val expected1 = buildAnonymousElectorDocument(
                finalRetentionRemovalDate = LocalDate.now().minusDays(1)
            )
            val expected2 = buildAnonymousElectorDocument(
                finalRetentionRemovalDate = LocalDate.now().minusDays(1)
            )
            val other = buildAnonymousElectorDocument(
                finalRetentionRemovalDate = LocalDate.now().plusDays(1)
            )

            anonymousElectorDocumentRepository.saveAll(listOf(expected1, expected2, other))

            // When
            val actual = anonymousElectorDocumentRepository.findPendingRemovalOfFinalRetentionData(ANONYMOUS_ELECTOR_DOCUMENT)

            // Then
            assertThat(actual).containsExactlyInAnyOrder(expected1, expected2)
        }
    }
}
