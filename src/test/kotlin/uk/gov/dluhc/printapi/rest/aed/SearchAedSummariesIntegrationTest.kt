package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.AedSearchBy
import uk.gov.dluhc.printapi.models.AedSearchBy.SURNAME
import uk.gov.dluhc.printapi.models.AedSearchSummaryResponse
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAedSearchSummaryApiFromAedEntity
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import java.time.Instant
import java.time.LocalDate

internal class SearchAedSummariesIntegrationTest : IntegrationTest() {
    companion object {
        private const val SEARCH_SUMMARY_URI_TEMPLATE = "/eros/{eroId}/anonymous-elector-documents/search"
        private const val GSS_CODE = "W06000099"
        private const val ANOTHER_GSS_CODE = "E06000123"
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()

        // When
        webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, searchBy = SURNAME))
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
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
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AedSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        with(actual!!) {
            assertThat(page).isEqualTo(0)
            assertThat(pageSize).isEqualTo(100)
            assertThat(totalPages).isZero()
            assertThat(totalResults).isZero()
            assertThat(results).isNotNull.isEmpty()
        }
    }

    @Test
    fun `should return all summaries given no pagination and no searchBy specified`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val currentDate = LocalDate.now()
        val currentDateTimeInstant = Instant.now()
        val aed1SourceReference = aValidSourceReference()
        val aed1ApplicationReference = aValidApplicationReference()
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
        val aed2ApplicationReference = aValidApplicationReference()
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

        val application3AedDocument = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate.plusDays(10))
        val application4AedDocument = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate)
        val otherGssCodeApplicationAed = buildAnonymousElectorDocument(gssCode = ANOTHER_GSS_CODE)

        anonymousElectorDocumentRepository.saveAll(
            listOf(
                application1InitialAed, application1SecondAed, application1LatestAed,
                application2InitialAed, application2SecondAed, application2LatestAed,
                application3AedDocument, application4AedDocument, otherGssCodeApplicationAed
            )
        )

        val expectedSummaryRecord1 = buildAedSearchSummaryApiFromAedEntity(application3AedDocument)
        val expectedSummaryRecord2 = buildAedSearchSummaryApiFromAedEntity(application4AedDocument)
        val expectedSummaryRecord3 = buildAedSearchSummaryApiFromAedEntity(application1LatestAed)
        val expectedSummaryRecord4 = buildAedSearchSummaryApiFromAedEntity(application2LatestAed)
        val expectedResultsInExactOrder = listOf(expectedSummaryRecord1, expectedSummaryRecord2, expectedSummaryRecord3, expectedSummaryRecord4)
        val expectedTotalPages = 1
        val expectedTotalResults = 4

        // When
        val response = webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AedSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        with(actual!!) {
            assertThat(page).isEqualTo(1)
            assertThat(pageSize).isEqualTo(100)
            assertThat(totalPages).isEqualTo(expectedTotalPages)
            assertThat(totalResults).isEqualTo(expectedTotalResults)
            assertThat(results).isNotEmpty
                .hasSize(4)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*dateTimeCreated")
                .isEqualTo(expectedResultsInExactOrder)
        }
    }

    @Test
    fun `should return empty list given no matching records found when searchBy surname`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val application1Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, surname = "Smi-th")
        val application2Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, surname = "BlackSmith")
        val application3Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, surname = "Sm1th")
        val application4Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, surname = "S mith")
        val otherGssCodeApplicationAed = buildAnonymousElectorDocument(gssCode = ANOTHER_GSS_CODE)

        anonymousElectorDocumentRepository.saveAll(
            listOf(
                application1Aed, application2Aed, application3Aed, application4Aed, otherGssCodeApplicationAed
            )
        )

        // When
        val response = webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID, searchBy = SURNAME, searchValue = "Smith"))
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AedSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        with(actual!!) {
            assertThat(page).isEqualTo(0)
            assertThat(pageSize).isEqualTo(100)
            assertThat(totalPages).isZero()
            assertThat(totalResults).isZero()
            assertThat(results).isNotNull.isEmpty()
        }
    }

    @Test
    fun `should return summaries matching surname given searchBy surname specified`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val currentDate = LocalDate.now()
        val matchingApplication1Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate.minusDays(2), surname = "Smith")
        // Although surname matches for below AED 2, however this record won't be returned due to pageSize = 2
        val matchingApplication2Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate.minusDays(5), surname = "Smith")
        val matchingApplication3Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate, surname = "S'mith")

        val unmatchedApplication1Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, surname = "BlackSmith")
        val unmatchedApplication2Aed = buildAnonymousElectorDocument(gssCode = GSS_CODE, issueDate = currentDate.plusDays(1))
        val otherGssCodeApplicationAed = buildAnonymousElectorDocument(gssCode = ANOTHER_GSS_CODE)

        anonymousElectorDocumentRepository.saveAll(
            listOf(
                matchingApplication1Aed, matchingApplication2Aed, matchingApplication3Aed,
                unmatchedApplication1Aed, unmatchedApplication2Aed, otherGssCodeApplicationAed
            )
        )

        val expectedSummaryRecord1 = buildAedSearchSummaryApiFromAedEntity(matchingApplication3Aed)
        val expectedSummaryRecord2 = buildAedSearchSummaryApiFromAedEntity(matchingApplication1Aed)
        val expectedResultsInExactOrder = listOf(expectedSummaryRecord1, expectedSummaryRecord2)
        val expectedTotalPages = 2
        val expectedTotalResults = 3

        val pageSize = 2
        val searchBySurnameValue = "Smith"

        // When
        val response = webTestClient.get()
            .uri(
                buildUri(
                    uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE,
                    eroId = ERO_ID,
                    page = 1,
                    pageSize = pageSize,
                    searchBy = SURNAME,
                    searchValue = searchBySurnameValue
                )
            )
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AedSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        with(actual!!) {
            assertThat(page).isEqualTo(1)
            assertThat(pageSize).isEqualTo(pageSize)
            assertThat(totalPages).isEqualTo(expectedTotalPages)
            assertThat(totalResults).isEqualTo(expectedTotalResults)
            assertThat(results).isNotEmpty
                .hasSize(2)
                .usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(".*dateTimeCreated")
                .isEqualTo(expectedResultsInExactOrder)
        }
    }

    private fun buildUri(
        uriTemplate: String,
        uriBuilder: UriComponentsBuilder = UriComponentsBuilder.fromUriString(uriTemplate),
        eroId: String = ERO_ID,
        page: Int? = null,
        pageSize: Int? = null,
        searchBy: AedSearchBy? = null,
        searchValue: String? = null,
    ): String {
        if (page != null) {
            uriBuilder.queryParam("page", page)
        }
        if (pageSize != null) {
            uriBuilder.queryParam("pageSize", pageSize)
        }
        if (searchBy != null) {
            uriBuilder.queryParam("searchBy", searchBy.value)
        }
        if (searchValue != null) {
            uriBuilder.queryParam("searchValue", searchValue)
        }

        return uriBuilder.buildAndExpand(mapOf("eroId" to eroId)).toUriString()
    }
}
