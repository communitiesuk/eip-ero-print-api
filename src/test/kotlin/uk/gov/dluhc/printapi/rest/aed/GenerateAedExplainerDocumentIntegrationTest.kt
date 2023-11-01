package uk.gov.dluhc.printapi.rest.aed

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse

internal class GenerateAedExplainerDocumentIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents/{GSS_CODE}/explainer-document"
        private const val OTHER_ERO_ID = "other-city-council"
        private const val GSS_CODE = "E99999999"
        private const val MAX_SIZE_1_MB = 1024 * 1024
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
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
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(NOT_FOUND)
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(NOT_FOUND.value())
            .hasError("Not Found")
            .hasMessage("Anonymous Elector Document explainer document not found for eroId $ERO_ID and gssCode $GSS_CODE")
    }

    @Test
    fun `should return not found given no local authority found for the provided ero and gss code`() {
        // Given
        val eroName = aValidLocalAuthorityName()
        val localAuthorities: List<LocalAuthorityResponse> = listOf(
            buildLocalAuthorityResponse(gssCode = GSS_CODE, contactDetailsEnglish = buildContactDetails(nameVac = eroName))
        )
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = OTHER_ERO_ID, localAuthorities = localAuthorities)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponse, GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isEqualTo(NOT_FOUND)
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(NOT_FOUND.value())
            .hasError("Not Found")
            .hasMessage("Anonymous Elector Document explainer document not found for eroId $ERO_ID and gssCode $GSS_CODE")
    }

    @Test
    fun `should return AED explainer document pdf given valid request for authorised user`() {
        // Given
        val eroName = aValidLocalAuthorityName()
        val localAuthorities: List<LocalAuthorityResponse> = listOf(
            buildLocalAuthorityResponse(gssCode = GSS_CODE, contactDetailsEnglish = buildContactDetails(nameVac = eroName))
        )
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID, localAuthorities = localAuthorities)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponse, GSS_CODE)

        // When
        val response = webTestClient.mutate()
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_SIZE_1_MB) }
            .build()
            .post()
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .exchange()

        // Then
        val result = response
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody(ByteArray::class.java)
            .returnResult()

        val contentDisposition = result.responseHeaders.contentDisposition
        assertThat(contentDisposition.filename).isEqualTo("AED-explainer-document-$GSS_CODE.pdf")

        val pdfContent = result.responseBody
        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(eroName)
        }
    }
}
