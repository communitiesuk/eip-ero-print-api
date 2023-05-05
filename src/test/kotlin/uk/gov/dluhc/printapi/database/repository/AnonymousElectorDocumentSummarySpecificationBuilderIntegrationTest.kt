package uk.gov.dluhc.printapi.database.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.dto.aed.AedSearchBy
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import java.time.Instant
import java.time.LocalDate

@Deprecated("This is a temporary test, and will be removed when EIP1-5612 uses the specification builder in the search service")
internal class AnonymousElectorDocumentSummarySpecificationBuilderIntegrationTest : IntegrationTest() {

    companion object {
        private const val GSS_CODE = "W06000099"
        private const val ANOTHER_GSS_CODE = "E06000123"
    }

    @Autowired
    private lateinit var specificationBuilder: AnonymousElectorDocumentSummarySpecificationBuilder

    @Test
    fun `should return all AEDs for gsscode given no search by criteria`() {
        // Given
        createAndSaveSomeAeds()

        val criteria = buildAnonymousSearchCriteriaDto(
            page = 1,
            pageSize = 100,
            searchBy = null,
            searchValue = null
        )

        val specification = specificationBuilder.buildSpecification(listOf(GSS_CODE), criteria)
        val pageRequest = PageRequest.of(
            criteria.page - 1, // pages are zero indexed
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "issueDate").and(Sort.by(Sort.Direction.ASC, "surname"))
        )

        // When
        val actual = anonymousElectorDocumentSummaryRepository.findAll(specification, pageRequest)

        // Then
        assertThat(actual.content.map { it.applicationReference }).containsExactly(
            "V_APP_REF3", // AED3 first because it's the most recent (today)
            "V_APP_REF1", // AED1 2nd because it has the same date as AED2 but the surname is AAA (9 days old)
            "V_APP_REF2", // AED2 3rd because it has the same date as AED1 but the surname is ZZZ (9 days old)
            "V_APP_REF4", // AED4 last because it's the oldest (10 days old)
        )
    }

    @Test
    fun `should return all AEDs for gsscode given surname search by criteria`() {
        // Given
        createAndSaveSomeAeds()

        val criteria = buildAnonymousSearchCriteriaDto(
            page = 1,
            pageSize = 100,
            searchBy = AedSearchBy.SURNAME,
            searchValue = "zZz"
        )

        val specification = specificationBuilder.buildSpecification(listOf(GSS_CODE), criteria)
        val pageRequest = PageRequest.of(
            criteria.page - 1, // pages are zero indexed
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "issueDate").and(Sort.by(Sort.Direction.ASC, "surname"))
        )

        // When
        val actual = anonymousElectorDocumentSummaryRepository.findAll(specification, pageRequest)

        // Then
        assertThat(actual.content.map { it.applicationReference }).containsExactly(
            "V_APP_REF2", // AED2 because it has the surname ZZZ
        )
    }

    @Test
    fun `should return all AEDs for gsscode given surname with spaces and mixed case search by criteria`() {
        // Given
        createAndSaveSomeAeds()

        val criteria = buildAnonymousSearchCriteriaDto(
            page = 1,
            pageSize = 100,
            searchBy = AedSearchBy.SURNAME,
            searchValue = "  o'LeaRY  "
        )

        val specification = specificationBuilder.buildSpecification(listOf(ANOTHER_GSS_CODE), criteria)
        val pageRequest = PageRequest.of(
            criteria.page - 1, // pages are zero indexed
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "issueDate").and(Sort.by(Sort.Direction.ASC, "surname"))
        )

        // When
        val actual = anonymousElectorDocumentSummaryRepository.findAll(specification, pageRequest)

        // Then
        assertThat(actual.content.map { it.applicationReference }).containsExactly(
            "V_APP_REF5", // AED5 because it has the surname O'Leary and is for gssCode ANOTHER_GSS_CODE
        )
    }

    @Test
    fun `should return all AEDs for gsscode given application_reference search by criteria`() {
        // Given
        createAndSaveSomeAeds()

        val criteria = buildAnonymousSearchCriteriaDto(
            page = 1,
            pageSize = 100,
            searchBy = AedSearchBy.APPLICATION_REFERENCE,
            searchValue = "V_APP_REF3"
        )

        val specification = specificationBuilder.buildSpecification(listOf(GSS_CODE), criteria)
        val pageRequest = PageRequest.of(
            criteria.page - 1, // pages are zero indexed
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "issueDate").and(Sort.by(Sort.Direction.ASC, "surname"))
        )

        // When
        val actual = anonymousElectorDocumentSummaryRepository.findAll(specification, pageRequest)

        // Then
        assertThat(actual.content.map { it.applicationReference }).containsExactly(
            "V_APP_REF3", // AED3 because it has the application reference V_APP_REF3
        )
    }

    @Test
    fun `should return no AEDs for gsscode given search by criteria that matches nothing`() {
        // Given
        createAndSaveSomeAeds()

        val criteria = buildAnonymousSearchCriteriaDto(
            page = 1,
            pageSize = 100,
            searchBy = AedSearchBy.SURNAME,
            searchValue = "Bloggs"
        )

        val specification = specificationBuilder.buildSpecification(listOf(GSS_CODE), criteria)
        val pageRequest = PageRequest.of(
            criteria.page - 1, // pages are zero indexed
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "issueDate").and(Sort.by(Sort.Direction.ASC, "surname"))
        )

        // When
        val actual = anonymousElectorDocumentSummaryRepository.findAll(specification, pageRequest)

        // Then
        assertThat(actual.content).isEmpty()
    }

    private fun createAndSaveSomeAeds() {
        val currentDate = LocalDate.now(clock)
        val currentDateTimeInstant = Instant.now(clock)

        val aed1SourceReference = aValidSourceReference()
        val aed1ApplicationReference = "V_APP_REF1"
        val application1InitialAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed1SourceReference,
            applicationReference = aed1ApplicationReference,
            issueDate = currentDate.minusDays(10),
            requestDateTime = currentDateTimeInstant.minusSeconds(10),
            surname = "AAA",
        )
        val application1SecondAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed1SourceReference,
            applicationReference = aed1ApplicationReference,
            issueDate = currentDate.minusDays(10),
            requestDateTime = currentDateTimeInstant.minusSeconds(9),
            contactDetails = application1InitialAed.contactDetails!!
        )
        val application1LatestAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed1SourceReference,
            applicationReference = aed1ApplicationReference,
            issueDate = currentDate.minusDays(9),
            requestDateTime = currentDateTimeInstant, // View will return this latest record as it has latest requestDateTime
            contactDetails = application1InitialAed.contactDetails!!
        )

        val aed2SourceReference = aValidSourceReference()
        val aed2ApplicationReference = "V_APP_REF2"
        val application2InitialAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed2SourceReference,
            applicationReference = aed2ApplicationReference,
            issueDate = currentDate.minusDays(10),
            requestDateTime = currentDateTimeInstant.minusSeconds(10),
            surname = "ZZZ",
        )
        val application2SecondAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed2SourceReference,
            applicationReference = aed2ApplicationReference,
            issueDate = currentDate.minusDays(9),
            requestDateTime = currentDateTimeInstant.minusSeconds(8),
            contactDetails = application2InitialAed.contactDetails!!
        )
        val application2LatestAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed2SourceReference,
            applicationReference = aed2ApplicationReference,
            issueDate = currentDate.minusDays(9),
            requestDateTime = currentDateTimeInstant, // View will return this latest record as it has latest requestDateTime
            contactDetails = application2InitialAed.contactDetails!!
        )

        val application3AedDocument = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate, applicationReference = "V_APP_REF3")
        val application4AedDocument = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate.minusDays(10), applicationReference = "V_APP_REF4")
        val application5AedDocument = buildAnonymousElectorDocument(gssCode = ANOTHER_GSS_CODE, surname = "O'Leary", applicationReference = "V_APP_REF5")

        anonymousElectorDocumentRepository.saveAll(
            listOf(
                application1InitialAed, application1SecondAed, application1LatestAed,
                application2InitialAed, application2SecondAed, application2LatestAed,
                application3AedDocument, application4AedDocument, application5AedDocument
            )
        )
    }
}
