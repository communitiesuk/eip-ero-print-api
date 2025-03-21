package uk.gov.dluhc.printapi.rest

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.util.ResourceUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.Tag
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.matchingPreSignedAwsS3GetUrl
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.withBody
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class GenerateTemporaryCertificateIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/temporary-certificates"
        private const val OTHER_ERO_ID = "other-city-council"
        private const val GSS_CODE = "W06000023"
        private const val CERTIFICATE_SAMPLE_PHOTO =
            "classpath:temporary-certificate-template/sample-certificate-photo.png"
        private const val LARGE_CERTIFICATE_SAMPLE_PHOTO =
            "classpath:temporary-certificate-template/sample-too-large-certificate-photo.png"
        private const val MAX_SIZE_20_MB = 20 * 1024 * 1024
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAdminBearerToken(eroId = userGroupEroId))
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
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
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
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
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

    @Test
    fun `should return not found given no local authority found for the provided gss code`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCodeNoMatch(GSS_CODE)
        val requestBody = buildGenerateTemporaryCertificateRequest(gssCode = GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
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
            .hasMessageContaining("Temporary Certificate gssCode '$GSS_CODE' does not exist")
    }

    @Test
    fun `should return not found given no local authority found for the provided ero and gss code`() {
        // Given
        val eroResponseHasDifferentEroIdToBearerTokenAndUri = buildElectoralRegistrationOfficeResponse(
            id = OTHER_ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponseHasDifferentEroIdToBearerTokenAndUri, GSS_CODE)
        val requestBody = buildGenerateTemporaryCertificateRequest(gssCode = GSS_CODE)

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
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
            .hasMessageContaining("Temporary Certificate gssCode '$GSS_CODE' is not valid for eroId '$ERO_ID'")
    }

    @Test
    fun `should create a temporary certificate and return a presigned url to S3 given valid request for authorised user`() {
        // Given
        val photoLocationArn = addPhotoToS3(CERTIFICATE_SAMPLE_PHOTO)
        val request = buildGenerateTemporaryCertificateRequest(photoLocation = photoLocationArn)
        val localAuthorityName = aValidLocalAuthorityName()
        val localAuthorities: List<LocalAuthorityResponse> = listOf(
            buildLocalAuthorityResponse(
                gssCode = request.gssCode,
                contactDetailsEnglish = buildContactDetails(nameVac = localAuthorityName)
            )
        )
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID, localAuthorities = localAuthorities)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponse, request.gssCode)

        // When
        val response = webTestClient.mutate()
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_SIZE_20_MB) }
            .build().post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(MediaType.APPLICATION_JSON)
            .withBody(request)
            .exchange()
            .expectStatus().isCreated
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val temporaryCertificates = temporaryCertificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(
            listOf(request.gssCode),
            VOTER_CARD,
            request.sourceReference
        )
        assertThat(temporaryCertificates).hasSize(1)
        val temporaryCertificate = temporaryCertificates[0]

        val responseBody = response.responseBody.blockFirst()
        val expectedS3Key =
            "${request.gssCode}/${request.sourceReference}/temporary-certificate-${temporaryCertificate.certificateNumber}.pdf"
        val expectedUrl = matchingPreSignedAwsS3GetUrl(expectedS3Key)
        assertThat(responseBody!!.preSignedUrl).matches {
            it.toString()
                .matches(expectedUrl)
        }

        val temporaryCertificateFromS3 = getObjectFromS3(expectedS3Key)
        val pdfContent = temporaryCertificateFromS3.bytes
        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(localAuthorityName)
        }

        assertThat(temporaryCertificateFromS3.contentType)
            .isEqualTo(MediaType.APPLICATION_PDF_VALUE)

        assertThat(temporaryCertificateFromS3.tags).isEqualTo(
            listOf(Tag.builder().key("ToDelete").value("True").build())
        )

        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertUpdateStatisticsMessageSent(request.sourceReference)
        }
    }

    private fun addPhotoToS3(filepath: String): String {
        val s3Resource = ResourceUtils.getFile(filepath).readBytes()
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
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
