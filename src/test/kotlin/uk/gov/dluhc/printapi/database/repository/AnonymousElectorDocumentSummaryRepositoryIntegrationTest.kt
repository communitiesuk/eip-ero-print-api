package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryViewFromAedEntity
import java.time.Instant
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
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = withPageRequestAndSortOrder(0, 100)
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
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = withPageRequestAndSortOrder(0, 100)
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should find all AEDs for a given gssCode and source type pages`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()
            val currentDateTimeInstant = Instant.now()
            val aed1SourceReference = aValidSourceReference()
            val aed1ApplicationReference = aValidApplicationReference()

            val aed1FirstCertificate = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(10),
                contactDetails = buildAedContactDetails(surname = "A")
            )
            val aed1SecondCertificate = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(9),
                contactDetails = aed1FirstCertificate.contactDetails!!
            )
            val aed1LatestCertificate = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant, // View will return this latest record
                contactDetails = aed1FirstCertificate.contactDetails!!
            )

            val aed2SourceReference = aValidSourceReference()
            val aed2ApplicationReference = aValidApplicationReference()
            val aed2FirstCertificate = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(10),
                contactDetails = buildAedContactDetails(surname = "Z")
            )
            val aed2SecondCertificate = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant.minusSeconds(8),
                contactDetails = aed2FirstCertificate.contactDetails!!
            )
            val aed2LatestCertificate = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant, // View will return this latest record
                contactDetails = aed2FirstCertificate.contactDetails!!
            )
            val aed3 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(10))
            val aed4 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(
                listOf(
                    aed1FirstCertificate, aed1SecondCertificate, aed1LatestCertificate,
                    aed2FirstCertificate, aed2SecondCertificate, aed2LatestCertificate,
                    aed3, aed4, otherAed
                )
            )

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed3)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed4)
            val expectedSummaryRecord3 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed1LatestCertificate)
            val expectedSummaryRecord4 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed2LatestCertificate)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = withPageRequestAndSortOrder(0, 100)
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

        @Test
        fun `should find AEDs for a given gssCode and source type for matching page`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()

            val aed1 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val aed2 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val aed3 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(1))
            val aed4 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(2))
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2, aed3, aed4, otherAed))

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed3)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(aed4)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = withPageRequestAndSortOrder(0, 2)
                )

            // Then
            assertThat(actual)
                .doesNotContain(otherAedSummaryRecord)
                .containsExactlyInAnyOrder(expectedSummaryRecord1, expectedSummaryRecord2)
        }

        @Test
        fun `should return empty AED results for non matching page request`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()

            val aed1 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val aed2 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val aed3 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(1))
            val aed4 = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(2))
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2, aed3, aed4, otherAed))

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = withPageRequestAndSortOrder(1, 5)
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        private fun withPageRequestAndSortOrder(page: Int, size: Int): PageRequest {
            val sortByIssueDateDesc = Sort.by(Sort.Direction.DESC, "issueDate")
            val sortBySurnameAsc = Sort.by(Sort.Direction.ASC, "surname")
            return PageRequest.of(page, size, sortByIssueDateDesc.and(sortBySurnameAsc))
        }
    }
}
