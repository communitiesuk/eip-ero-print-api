package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.AedSearchSummaryResponse
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAedSearchSummaryApiFromAedEntity
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import java.time.Instant
import java.time.LocalDate

internal class SearchAedSummariesIntegrationTest : IntegrationTest() {
    companion object {
        private const val SEARCH_SUMMARY_URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents/search"
        private const val GSS_CODE = "W06000099"
        private const val ANOTHER_GSS_CODE = "E06000123"
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.get()
            .uri(SEARCH_SUMMARY_URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return empty list given no AEDs exist for an ERO`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        // When
        val response = webTestClient.get()
            .uri(SEARCH_SUMMARY_URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AedSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual!!.results).isEmpty()
    }

    @Test
    fun `should return summaries given AEDs exist for an ERO`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        val currentDate = LocalDate.now()
        val currentDateTimeInstant = Instant.now()
        val aed1SourceReference = aValidSourceReference()
        val aed1ApplicationReference = aValidApplicationReference()
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val application1InitialAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed1SourceReference,
            applicationReference = aed1ApplicationReference,
            issueDate = currentDate.minusDays(10),
            requestDateTime = currentDateTimeInstant.minusSeconds(10),
            contactDetails = buildAedContactDetails(surname = "A")
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
        val aed2ApplicationReference = aValidApplicationReference()
        val application2InitialAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = aed2SourceReference,
            applicationReference = aed2ApplicationReference,
            issueDate = currentDate.minusDays(10),
            requestDateTime = currentDateTimeInstant.minusSeconds(10),
            contactDetails = buildAedContactDetails(surname = "Z")
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

        val application3AedDocument = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate.plusDays(10))
        val application4AedDocument = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate)
        val otherApplicationAed = buildAnonymousElectorDocument(gssCode = ANOTHER_GSS_CODE)

        anonymousElectorDocumentRepository.saveAll(
            listOf(
                application1InitialAed, application1SecondAed, application1LatestAed,
                application2InitialAed, application2SecondAed, application2LatestAed,
                application3AedDocument, application4AedDocument, otherApplicationAed
            )
        )

        val expectedSummaryRecord1 = buildAedSearchSummaryApiFromAedEntity(application3AedDocument)
        val expectedSummaryRecord2 = buildAedSearchSummaryApiFromAedEntity(application4AedDocument)
        val expectedSummaryRecord3 = buildAedSearchSummaryApiFromAedEntity(application1LatestAed)
        val expectedSummaryRecord4 = buildAedSearchSummaryApiFromAedEntity(application2LatestAed)

        val expectedResults = listOf(expectedSummaryRecord1, expectedSummaryRecord2, expectedSummaryRecord3, expectedSummaryRecord4)

        // When
        val response = webTestClient.get()
            .uri(SEARCH_SUMMARY_URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AedSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual!!.results).isNotEmpty
        assertThat(actual.results)
            .usingRecursiveComparison()
            .ignoringFieldsMatchingRegexes(".*dateTimeCreated")
            .isEqualTo(expectedResults)
    }
}
