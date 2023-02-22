package uk.gov.dluhc.printapi.rest

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.util.ResourceUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.withBody
import java.io.ByteArrayInputStream
import java.util.UUID

internal class GenerateAnonymousElectorDocumentIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents"
        private const val ERO_ID = "some-city-council"
        private const val OTHER_ERO_ID = "other-city-council"
        private const val GSS_CODE = "W06000023"
        private const val AED_SAMPLE_PHOTO = "classpath:temporary-certificate-template/sample-certificate-photo.png"
        private const val MAX_SIZE_2_MB = 2 * 1024 * 1024
    }

    @Test
    fun `should return forbidden given user with bearer token missing required group to access service`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.post()
            .uri(
                URI_TEMPLATE,
                ERO_ID,
                GSS_CODE
            )
            .bearerToken(
                getBearerToken(
                    eroId = userGroupEroId,
                    groups = listOf("ero-$userGroupEroId", "ero-vc-anonymous-admin-$userGroupEroId")
                )
            )
            .contentType(MediaType.APPLICATION_JSON)
            .withBody(buildGenerateAnonymousElectorDocumentRequest())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request body`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val invalidGssCode = "invalid GSS Code"
        val requestBody = buildGenerateAnonymousElectorDocumentRequest(
            gssCode = invalidGssCode
        )

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(
                getBearerToken(
                    eroId = ERO_ID,
                    groups = listOf("ero-$ERO_ID", "ero-vc-anonymous-admin-$ERO_ID")
                )
            )
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
            .hasMessageContaining("Validation failed for object='generateAnonymousElectorDocumentRequest'. Error count: 1")
            .hasValidationError("Error on field 'gssCode': rejected value [$invalidGssCode], size must be between 9 and 9")
    }

    @Test
    fun `should return not found given no local authority found for the provided gss code`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCodeNoMatch(GSS_CODE)
        val requestBody = buildGenerateAnonymousElectorDocumentRequest(gssCode = GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(
                URI_TEMPLATE,
                ERO_ID
            )
            .bearerToken(
                getBearerToken(
                    eroId = ERO_ID,
                    groups = listOf("ero-$ERO_ID", "ero-vc-anonymous-admin-$ERO_ID")
                )
            )
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
            .hasMessageContaining("Anonymous Elector Document gssCode '$GSS_CODE' does not exist")
    }

    @Test
    fun `should return not found given no local authority found for the provided ero and gss code`() {
        // Given
        val eroResponseHasDifferentEroIdToBearerTokenAndUri = buildElectoralRegistrationOfficeResponse(
            id = OTHER_ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(
            eroResponseHasDifferentEroIdToBearerTokenAndUri,
            GSS_CODE
        )
        val requestBody = buildGenerateAnonymousElectorDocumentRequest(gssCode = GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(
                URI_TEMPLATE,
                ERO_ID
            )
            .bearerToken(
                getBearerToken(
                    eroId = ERO_ID,
                    groups = listOf("ero-$ERO_ID", "ero-vc-anonymous-admin-$ERO_ID")
                )
            )
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
            .hasMessageContaining("Anonymous Elector Document gssCode '$GSS_CODE' is not valid for eroId '$ERO_ID'")
    }

    @Test
    fun `should return AED pdf given valid request for authorised user`() {
        // Given
        val photoLocationArn = addPhotoToS3()
        val request = buildGenerateAnonymousElectorDocumentRequest(photoLocation = photoLocationArn)
        val localAuthorities: List<LocalAuthorityResponse> = listOf(
            buildLocalAuthorityResponse(
                gssCode = request.gssCode,
            )
        )
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID, localAuthorities = localAuthorities)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponse, request.gssCode)

        // When
        val response = webTestClient.mutate()
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_SIZE_2_MB) }
            .build().post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(
                getBearerToken(
                    eroId = ERO_ID,
                    groups = listOf("ero-$ERO_ID", "ero-vc-anonymous-admin-$ERO_ID")
                )
            )
            .contentType(MediaType.APPLICATION_JSON)
            .withBody(request)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody(ByteArray::class.java)
            .returnResult()

        // Then
        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(
            listOf(request.gssCode),
            VOTER_CARD,
            request.sourceReference
        )
        assertThat(electorDocuments).hasSize(1)
        val electorDocument = electorDocuments[0]
        val contentDisposition = response.responseHeaders.contentDisposition
        assertThat(contentDisposition.filename)
            .isEqualTo("anonymous-elector-document-${electorDocument.certificateNumber}.pdf")

        val pdfContent = response.responseBody
        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(electorDocument.certificateNumber)
        }
    }

    private fun addPhotoToS3(): String {
        val s3Resource = ResourceUtils.getFile(AED_SAMPLE_PHOTO).readBytes()
        val s3Bucket = LocalStackContainerConfiguration.S3_BUCKET_CONTAINING_PHOTOS
        val s3Path = "E09000007/0013a30ac9bae2ebb9b1239b/${UUID.randomUUID()}/8a53a30ac9bae2ebb9b1239b-test-photo.png"

        // add resource to S3
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Path)
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )
        return "arn:aws:s3:::$s3Bucket/$s3Path"
    }
}
