package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.testsupport.buildSanitizedSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.anotherGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryViewFromAedEntity
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPageRequest
import java.time.Instant
import java.time.LocalDate

internal class AnonymousElectorDocumentSummaryRepositoryIntegrationTest : IntegrationTest() {

    @Nested
    inner class FindAllByGssCodeInAndSourceType {

        @Test
        fun `should return empty list when summary view has no records`() {
            // Given
            val gssCode = aGssCode()

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should return empty list when no matching gssCode record exists`() {
            // Given
            val gssCode = aGssCode()
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())
            anonymousElectorDocumentRepository.saveAll(listOf(otherAed))

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should find all AEDs for a given gssCode and source type`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()
            val currentDateTimeInstant = Instant.now()
            val aed1SourceReference = aValidSourceReference()
            val aed1ApplicationReference = aValidApplicationReference()

            val application1InitialAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(10),
                surname = "AAA",
            )
            val application1SecondAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(9),
                contactDetails = application1InitialAed.contactDetails!!
            )
            val application1LatestAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant, // View will return this latest record as it has latest requestDateTime
                contactDetails = application1InitialAed.contactDetails!!
            )

            val aed2SourceReference = aValidSourceReference()
            val aed2ApplicationReference = aValidApplicationReference()
            val application2InitialAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(10),
                surname = "ZZZ",
            )
            val application2SecondAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant.minusSeconds(8),
                contactDetails = application2InitialAed.contactDetails!!
            )
            val application2LatestAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant, // View will return this latest record as it has latest requestDateTime
                contactDetails = application2InitialAed.contactDetails!!
            )
            val application3AedDocument = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(10))
            val application4AedDocument = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val otherApplicationAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(
                listOf(
                    application1InitialAed, application1SecondAed, application1LatestAed,
                    application2InitialAed, application2SecondAed, application2LatestAed,
                    application3AedDocument, application4AedDocument, otherApplicationAed
                )
            )

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application3AedDocument)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application4AedDocument)
            val expectedSummaryRecord3 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application1LatestAed)
            val expectedSummaryRecord4 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application2LatestAed)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherApplicationAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual)
                .doesNotContain(otherAedSummaryRecord)
                .containsExactly(
                    expectedSummaryRecord1,
                    expectedSummaryRecord2,
                    expectedSummaryRecord3,
                    expectedSummaryRecord4
                )
        }

        @Test
        fun `should find AEDs for a given gssCode and source type for a matching page`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()

            val application1Aed = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.minusDays(2))
            val application2Aed = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.minusDays(1))
            val application3Aed = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate.plusDays(1))
            val application4Aed = buildAnonymousElectorDocument(gssCode = gssCode, issueDate = currentDate)
            val otherApplicationAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(listOf(application1Aed, application2Aed, application3Aed, application4Aed, otherApplicationAed))

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application3Aed)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application4Aed)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherApplicationAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = buildPageRequest(1, 2)
                )

            // Then
            assertThat(actual)
                .doesNotContain(otherAedSummaryRecord)
                .containsExactly(expectedSummaryRecord1, expectedSummaryRecord2)
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
                    pageRequest = buildPageRequest(2, 5)
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }
    }

    @Nested
    inner class FindAllByGssCodeInAndSourceTypeAndSanitizedSurname {

        @Test
        fun `should return empty list when summary view has no records`() {
            // Given
            val gssCode = aGssCode()

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sanitizedSurname = buildSanitizedSurname(aValidSurname()),
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should return empty list when no matching gssCode record exists`() {
            // Given
            val gssCode = aGssCode()
            val surname = aValidSurname()
            val otherAed = buildAnonymousElectorDocument(
                gssCode = anotherGssCode(),
                surname = surname,
            )
            anonymousElectorDocumentRepository.saveAll(listOf(otherAed))

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sanitizedSurname = buildSanitizedSurname(surname),
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should return empty list when no matching surname record exists`() {
            // Given
            val gssCode = aGssCode()
            val surname = aValidSurname()
            val anotherSurname = aValidSurname()
            val otherAed = buildAnonymousElectorDocument(
                gssCode = anotherGssCode(),
                surname = anotherSurname,
            )
            anonymousElectorDocumentRepository.saveAll(listOf(otherAed))

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sanitizedSurname = buildSanitizedSurname(surname),
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }

        @Test
        fun `should find all AEDs for a given gssCode and source type and sanitizedSurname`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()
            val currentDateTimeInstant = Instant.now()
            val aed1SourceReference = aValidSourceReference()
            val aed1ApplicationReference = aValidApplicationReference()
            val surname = aValidSurname()

            val application1InitialAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(10),
                surname = surname,
            )
            val application1SecondAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(9),
                contactDetails = application1InitialAed.contactDetails!!
            )
            val application1LatestAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed1SourceReference,
                applicationReference = aed1ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant, // View will return this latest record as it has latest requestDateTime
                contactDetails = application1InitialAed.contactDetails!!
            )

            val aed2SourceReference = aValidSourceReference()
            val aed2ApplicationReference = aValidApplicationReference()
            val application2InitialAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(10),
                requestDateTime = currentDateTimeInstant.minusSeconds(10),
                surname = surname,
            )
            val application2SecondAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant.minusSeconds(8),
                contactDetails = application2InitialAed.contactDetails!!
            )
            val application2LatestAed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                sourceReference = aed2SourceReference,
                applicationReference = aed2ApplicationReference,
                issueDate = currentDate.minusDays(9),
                requestDateTime = currentDateTimeInstant, // View will return this latest record as it has latest requestDateTime
                contactDetails = application2InitialAed.contactDetails!!
            )
            val application3AedDocument = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.plusDays(10),
                surname = surname,
            )
            val application4AedDocument = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate,
                surname = surname,
            )
            val otherApplicationAed = buildAnonymousElectorDocument(
                gssCode = anotherGssCode(),
                surname = aValidSurname(),
            )

            anonymousElectorDocumentRepository.saveAll(
                listOf(
                    application1InitialAed, application1SecondAed, application1LatestAed,
                    application2InitialAed, application2SecondAed, application2LatestAed,
                    application3AedDocument, application4AedDocument, otherApplicationAed
                )
            )

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application3AedDocument)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application4AedDocument)
            val expectedSummaryRecord3 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application1LatestAed)
            val expectedSummaryRecord4 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application2LatestAed)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherApplicationAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sanitizedSurname = buildSanitizedSurname(surname),
                    pageRequest = buildPageRequest()
                )

            // Then
            assertThat(actual)
                .doesNotContain(otherAedSummaryRecord)
                .containsExactly(
                    expectedSummaryRecord1,
                    expectedSummaryRecord2,
                    expectedSummaryRecord3,
                    expectedSummaryRecord4
                )
        }

        @Test
        fun `should find AEDs for a given gssCode and source type and sanitizedSurname for a matching page`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()
            val surname = aValidSurname()

            val application1Aed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.minusDays(2),
                surname = surname,
            )
            val application2Aed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.minusDays(1),
                surname = surname,
            )
            val application3Aed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.plusDays(1),
                surname = surname,
            )
            val application4Aed = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate,
                surname = surname,
            )
            val otherApplicationAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(listOf(application1Aed, application2Aed, application3Aed, application4Aed, otherApplicationAed))

            val expectedSummaryRecord1 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application3Aed)
            val expectedSummaryRecord2 = buildAnonymousElectorDocumentSummaryViewFromAedEntity(application4Aed)
            val otherAedSummaryRecord = buildAnonymousElectorDocumentSummaryViewFromAedEntity(otherApplicationAed)

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sanitizedSurname = buildSanitizedSurname(surname),
                    pageRequest = buildPageRequest(1, 2)
                )

            // Then
            assertThat(actual)
                .doesNotContain(otherAedSummaryRecord)
                .containsExactly(expectedSummaryRecord1, expectedSummaryRecord2)
        }

        @Test
        fun `should return empty AED results for non matching page request`() {
            // Given
            val gssCode = aGssCode()
            val currentDate = LocalDate.now()
            val surname = aValidSurname()

            val aed1 = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate,
                surname = surname,
            )
            val aed2 = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate,
                surname = surname,
            )
            val aed3 = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.plusDays(1),
                surname = surname,
            )
            val aed4 = buildAnonymousElectorDocument(
                gssCode = gssCode,
                issueDate = currentDate.plusDays(2),
                surname = surname,
            )
            val otherAed = buildAnonymousElectorDocument(gssCode = anotherGssCode())

            anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2, aed3, aed4, otherAed))

            // When
            val actual = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                    gssCodes = listOf(gssCode),
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    sanitizedSurname = buildSanitizedSurname(surname),
                    pageRequest = buildPageRequest(2, 5)
                )

            // Then
            assertThat(actual).isNotNull.isEmpty()
        }
    }
}
