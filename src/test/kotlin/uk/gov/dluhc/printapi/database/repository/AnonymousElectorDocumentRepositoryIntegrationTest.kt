package uk.gov.dluhc.printapi.database.repository

import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.transaction.TestTransaction
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfFinalRetentionData
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepositoryExtensions.findPendingRemovalOfInitialRetentionData
import uk.gov.dluhc.printapi.testsupport.testdata.anAuthenticatedJwtAuthenticationToken
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
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

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    open inner class UpdateContactDetails {
        @Test
        @Transactional
        open fun `should maintain updatedBy and dateUpdated fields when AED contact details are updated`() {
            // Given
            SecurityContextHolder.getContext().authentication = anAuthenticatedJwtAuthenticationToken("a-user@some-ero.gov.uk")

            val aed = buildAnonymousElectorDocument(
                contactDetails = buildAedContactDetails(
                    email = "initial-email@somewhere.com",
                    phoneNumber = "01111 111111"
                )
            )
            anonymousElectorDocumentRepository.save(aed)
            TestTransaction.flagForCommit()
            TestTransaction.end()

            val retrievedAed = anonymousElectorDocumentRepository.findById(aed.id!!).get()
            with(retrievedAed.contactDetails!!) {
                // Assert initial AED Contact Details are saved with expected JPA field values
                assertThat(createdBy).isEqualTo("a-user@some-ero.gov.uk")
                assertThat(updatedBy).isEqualTo("a-user@some-ero.gov.uk")
                assertThat(dateCreated).isEqualTo(dateUpdated)
                assertThat(version).isEqualTo(0)
            }

            Thread.sleep(1000) // Sleep for a second to be able to assert a difference between dateCreated and dateUpdated

            // When
            TestTransaction.start()
            SecurityContextHolder.getContext().authentication = anAuthenticatedJwtAuthenticationToken("another-user@some-ero.gov.uk")
            with(retrievedAed.contactDetails!!) {
                email = "an-updated-email@somewhere.com"
                phoneNumber = "02222 222222"
            }
            anonymousElectorDocumentRepository.save(retrievedAed) // save the updated AED
            TestTransaction.flagForCommit()
            TestTransaction.end()

            // Then
            SecurityContextHolder.clearContext()
            val updatedAed = anonymousElectorDocumentRepository.findById(aed.id!!).get()
            with(updatedAed.contactDetails!!) {
                // Assert updated AED Contact Details have expected updated JPA field values
                assertThat(createdBy).isEqualTo("a-user@some-ero.gov.uk")
                assertThat(updatedBy).isEqualTo("another-user@some-ero.gov.uk")
                assertThat(dateUpdated).isAfter(dateCreated)
                assertThat(version).isEqualTo(1)
            }
        }
    }
}
