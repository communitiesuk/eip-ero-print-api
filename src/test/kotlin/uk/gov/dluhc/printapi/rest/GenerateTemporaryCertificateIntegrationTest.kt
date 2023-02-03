package uk.gov.dluhc.printapi.rest

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.UNAUTHORIZED_BEARER_TOKEN
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken

internal class GenerateTemporaryCertificateIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/temporary-certificate"
        private const val ERO_ID = "some-city-council"
    }

    @Test
    fun `should return forbidden given no bearer token`() {
        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return unauthorized given user with invalid bearer token`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(UNAUTHORIZED_BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different group`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-admin-$ERO_ID")))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(
                getBearerToken(
                    eroId = userGroupEroId,
                    groups = listOf("ero-$userGroupEroId", "ero-vc-admin-$userGroupEroId")
                )
            )
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }
}
