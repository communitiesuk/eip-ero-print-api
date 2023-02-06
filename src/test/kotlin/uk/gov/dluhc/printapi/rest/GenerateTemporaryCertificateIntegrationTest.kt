package uk.gov.dluhc.printapi.rest

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.UNAUTHORIZED_BEARER_TOKEN
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.testsupport.withBody
import java.time.LocalDate

internal class GenerateTemporaryCertificateIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/temporary-certificate"
        private const val ERO_ID = "some-city-council"
    }

    @Test
    fun `should return forbidden given no bearer token`() {
        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .withBody(buildGenerateTemporaryCertificateRequest())
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
            .withBody(buildGenerateTemporaryCertificateRequest())
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
            .withBody(buildGenerateTemporaryCertificateRequest())
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
            .withBody(buildGenerateTemporaryCertificateRequest())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request body`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val invalidGssCode = "invalid GSS Code"
        val requestBody = buildGenerateTemporaryCertificateRequest(
            gssCode = invalidGssCode
        )

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
            .contentType(MediaType.APPLICATION_JSON)
            .withBody(requestBody)
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessageContaining("Validation failed for object='generateTemporaryCertificateRequest'. Error count: 1")
            .hasValidationError("Error on field 'gssCode': rejected value [$invalidGssCode], size must be between 9 and 9")
    }

    @Test
    fun `should return bad request given validOnDate fails business rules validation`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val yesterday = LocalDate.now(clock).minusDays(1)
        val requestBody = buildGenerateTemporaryCertificateRequest(
            validOnDate = yesterday
        )

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
            .contentType(MediaType.APPLICATION_JSON)
            .withBody(requestBody)
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessageContaining("Temporary Certificate validOnDate cannot be in the past")
    }
}
