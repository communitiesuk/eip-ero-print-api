package uk.gov.dluhc.printapi.rest.aed

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildUpdateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.withBody

internal class UpdateAnonymousElectorDocumentByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents"
        private const val GSS_CODE = "W06000099"
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .withBody(buildUpdateAnonymousElectorDocumentRequest())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request body`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val invalidRequestBody = buildUpdateAnonymousElectorDocumentRequest(
            email = null,
            phoneNumber = null
        )

        // When
        val response = webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(invalidRequestBody)
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessageContaining("Either email or phoneNumber must be provided")
    }

    @Test
    fun `should return not found given no AEDs exist for application`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val unknownSourceReference = aValidSourceReference()

        // When
        val response = webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(buildUpdateAnonymousElectorDocumentRequest(sourceReference = unknownSourceReference))
            .exchange()
            .expectStatus()
            .isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("Certificate for eroId = $ERO_ID with sourceType = ANONYMOUS_ELECTOR_DOCUMENT and sourceReference = $unknownSourceReference not found")
    }
}
