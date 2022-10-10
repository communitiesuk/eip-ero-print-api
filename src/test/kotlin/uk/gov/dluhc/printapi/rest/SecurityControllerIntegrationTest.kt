package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken

// TODO - delete this test when we implement controllers and their tests for stories
internal class SecurityControllerIntegrationTest : IntegrationTest() {

    @Test
    fun `should return principal name given request with bearer token`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val eroId = aValidRandomEroId()

        val request = webTestClient.get()
            .uri("/secured-endpoint")
            .bearerToken(
                getBearerToken(
                    eroId = eroId,
                    email = "an-ero-user@$eroId.gov.uk"
                )
            )

        // When
        val response = request.exchange()

        // Then
        val responseBody = response
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody
        assertThat(responseBody).isEqualTo("Hello an-ero-user@$eroId.gov.uk")
    }

    @Test
    fun `should return unauthorized given no bearer token`() {
        // Given
        val request = webTestClient.get().uri("/secured-endpoint")

        // When
        val response = request.exchange()

        // Then
        response.expectStatus().isUnauthorized
    }
}
