package uk.gov.dluhc.printapi.rest

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.UNAUTHORIZED_BEARER_TOKEN
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken

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
internal class SpringSecurityIntegrationTest : IntegrationTest() {

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
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different group`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-admin-$ERO_ID")))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }
}
