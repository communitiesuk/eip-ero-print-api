package uk.gov.dluhc.printapi.rest

import com.lowagie.text.pdf.PdfReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.UNAUTHORIZED_BEARER_TOKEN
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse

internal class GenerateTemporaryCertificateExplainerDocumentIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/temporary-certificate/{GSS_CODE}/explainer-document"
        private const val ERO_ID = "some-city-council"
        private const val GSS_CODE = "E99999999"
    }

    @Test
    fun `should return unauthorized given no bearer token`() {
        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return unauthorized given user with invalid bearer token`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
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
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
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
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
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

    @Test
    fun `should return not found given no local authority found for the provided gss code`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCodeNoMatch(GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()

        // Then
        response.expectStatus().isNotFound
        val actual = response.returnResult(String::class.java).responseBody.blockFirst()
        assertThat(actual).isEqualTo("Temporary certificate explainer document not found for gssCode $GSS_CODE")
    }

    @Test
    fun `should return temporary certificate pdf document given valid request for authorised user`() {
        // Given
        val eroName = aValidLocalAuthorityName()
        val localAuthorities: List<LocalAuthorityResponse> = listOf(
            buildLocalAuthorityResponse(gssCode = GSS_CODE, contactDetailsEnglish = buildContactDetails(name = eroName))
        )
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID, localAuthorities = localAuthorities)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponse, GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
            .exchange()

        // Then
        val result = response
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody(ByteArray::class.java)
            .returnResult()

        val contentDisposition = result.responseHeaders.contentDisposition
        assertThat(contentDisposition.filename).isEqualTo("temporary-certificate-explainer-document-$GSS_CODE.pdf")

        val pdfContent = result.responseBody
        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            assertThat(reader.acroFields.getField("eroName")).isEqualTo(eroName)
        }
    }
}
