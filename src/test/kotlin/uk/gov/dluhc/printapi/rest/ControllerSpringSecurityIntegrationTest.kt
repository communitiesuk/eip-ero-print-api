package uk.gov.dluhc.printapi.rest

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.UNAUTHORIZED_BEARER_TOKEN
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerTokenWithAllRolesExcept
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken

/**
 * Integration test that tests the basic spring security configuration concerns that are common across all API endpoints.
 * Specifically:
 * - A web request presented with no bearer token
 * - A web request presented with an invalid/corrupt bearer token.
 * - A web request presented with a bearer token that is not in the required group
 *
 * Security is a cross-cutting concern across all REST APIs so does not need to be repeated for every REST API endpoint. We
 * only need to test this once.
 */
internal class ControllerSpringSecurityIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/certificates/applications/{APPLICATION_ID}"
        private const val ERO_ID = "some-city-council"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4aa3"
    }

    @Test
    fun `should return unauthorized given no bearer token`() {
        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should return unauthorized given user with invalid bearer token`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(UNAUTHORIZED_BEARER_TOKEN)
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should return forbidden given user with valid bearer token not belonging to vc-admin group`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getBearerTokenWithAllRolesExcept(eroId = ERO_ID, excludedRoles = listOf("ero-vc-admin")))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return forbidden given user with valid bearer token not belonging to vc-anonymous-admin group`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri("/eros/{ERO_ID}/anonymous-elector-documents?applicationId={APPLICATION_ID}", ERO_ID, APPLICATION_ID)
            .bearerToken(getBearerTokenWithAllRolesExcept(eroId = ERO_ID, excludedRoles = listOf("ero-vc-anonymous-admin")))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad gateway error when ero service throws exception given valid user`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroThrowsInternalServerError()

        // When
        val response = webTestClient.get()
            .uri("/eros/{ERO_ID}/anonymous-elector-documents?applicationId={APPLICATION_ID}", ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.BAD_GATEWAY)
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(502)
            .hasError("Bad Gateway")
            .hasMessageContaining("Error retrieving GSS codes")
    }
}
