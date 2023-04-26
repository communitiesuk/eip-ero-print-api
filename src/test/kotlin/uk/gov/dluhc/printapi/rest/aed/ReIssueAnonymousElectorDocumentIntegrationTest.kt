package uk.gov.dluhc.printapi.rest.aed

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.util.ResourceUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.withBody
import java.io.ByteArrayInputStream
import java.util.UUID

internal class ReIssueAnonymousElectorDocumentIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents/re-issue"
        private const val GSS_CODE = "W06000099"
        private const val AED_SAMPLE_PHOTO = "classpath:temporary-certificate-template/sample-certificate-photo.png"
        private const val MAX_SIZE_2_MB = 2 * 1024 * 1024
    }

    @Test
    fun `should return forbidden given user with bearer token missing required group to access service`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .withBody(buildReIssueAnonymousElectorDocumentRequest())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request body`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val invalidElectoralRollNumber = "an invalid electoral roll number"
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            electoralRollNumber = invalidElectoralRollNumber
        )

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
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
            .hasMessageContaining("Validation failed for object='reIssueAnonymousElectorDocumentRequest'. Error count: 1")
            .hasValidationError("Error on field 'electoralRollNumber': rejected value [$invalidElectoralRollNumber], size must be between 1 and 25")
    }

    @Test
    fun `should return not found given specified application has no previously issued AEDs`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference
        )

        // When
        val response = webTestClient.post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(requestBody)
            .exchange()
            .expectStatus()
            .isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("Certificate for eroId = $ERO_ID with sourceType = ANONYMOUS_ELECTOR_DOCUMENT and sourceReference = $sourceReference not found")
    }

    @Test
    fun `should return AED PDF given specified application has previously issued AED`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()

        val previousAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
        )
        anonymousElectorDocumentRepository.save(previousAed)

        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference
        )

        // When
        val response = webTestClient.mutate()
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_SIZE_2_MB) }
            .build()
            .post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(requestBody)
            .exchange()
            .expectStatus().isCreated
            .expectHeader().contentType(MediaType.APPLICATION_PDF)
            .expectBody(ByteArray::class.java)
            .returnResult()

        // Then
        val pdfContent = response.responseBody
        val contentDisposition = response.responseHeaders.contentDisposition

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )
        assertThat(electorDocuments)
            .hasSize(2) // Exactly 2 AEDs expected in the database for this sourceReference
            .anyMatch { aed ->
                // Of the 2 AEDs in the database the response PDF filename will match only one of them.
                contentDisposition.filename == "anonymous-elector-document-${aed.certificateNumber}.pdf"
            }

        val newlyCreatedAed = electorDocuments.first { aed -> // Get the new AED that this test would have created
            contentDisposition.filename == "anonymous-elector-document-${aed.certificateNumber}.pdf"
        }
        assertThat(newlyCreatedAed)
            .hasId()
            .statusHistory {
                it.hasSize(1)
                it.hasStatus(AnonymousElectorDocumentStatus.Status.PRINTED)
            }

        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(newlyCreatedAed.certificateNumber)
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
