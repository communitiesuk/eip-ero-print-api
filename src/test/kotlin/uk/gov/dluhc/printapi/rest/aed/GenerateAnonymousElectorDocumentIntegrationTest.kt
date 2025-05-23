package uk.gov.dluhc.printapi.rest.aed

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.util.ResourceUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.Tag
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.matchingPreSignedAwsS3GetUrl
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAddress
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildApiCertificateDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildGenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildValidAddress
import uk.gov.dluhc.printapi.testsupport.withBody
import java.io.ByteArrayInputStream
import java.util.UUID.randomUUID
import java.util.concurrent.TimeUnit
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity
import uk.gov.dluhc.printapi.models.DeliveryAddressType as DeliveryAddressTypeApi
import uk.gov.dluhc.printapi.models.DeliveryClass as DeliveryClassApi

internal class GenerateAnonymousElectorDocumentIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents"
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
            .uri(URI_TEMPLATE, ERO_ID, GSS_CODE)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
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
        wireMockService.stubEroManagementGetEroByGssCode(eroResponseHasDifferentEroIdToBearerTokenAndUri, GSS_CODE)
        val requestBody = buildGenerateAnonymousElectorDocumentRequest(gssCode = GSS_CODE)

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
            .hasMessageContaining("Anonymous Elector Document gssCode '$GSS_CODE' is not valid for eroId '$ERO_ID'")
    }

    @Test
    fun `should return AED pdf given valid request for authorised user`() {
        // Given
        val photoLocationArn = addPhotoToS3()
        val collectionReason = "I don't trust Royal Mail"
        val request = buildGenerateAnonymousElectorDocumentRequest(
            supportingInformationFormat = AnonymousSupportingInformationFormat.LARGE_MINUS_PRINT,
            photoLocation = photoLocationArn,
            delivery = buildApiCertificateDelivery(
                deliveryClass = DeliveryClassApi.STANDARD,
                deliveryAddressType = DeliveryAddressTypeApi.ERO_MINUS_COLLECTION,
                collectionReason = collectionReason,
                addressFormat = uk.gov.dluhc.printapi.models.AddressFormat.UK
            )
        )
        processValidRequest(
            request = request,
            expectedSupportingFormat = SupportingInformationFormat.LARGE_PRINT,
            expectedAddressType = DeliveryAddressTypeEntity.ERO_COLLECTION,
            expectedCollectionReason = collectionReason
        )
    }

    @Test
    fun `should return AED pdf given valid request for authorised user with all optional data omitted`() {
        // Given
        val photoLocationArn = addPhotoToS3()
        val request = buildGenerateAnonymousElectorDocumentRequest(
            photoLocation = photoLocationArn,
            email = null, phoneNumber = null,
            registeredAddress = buildValidAddress(
                property = null, locality = null, town = null, area = null, uprn = null
            ),
            delivery = buildApiCertificateDelivery(
                deliveryClass = uk.gov.dluhc.printapi.models.DeliveryClass.STANDARD,
                deliveryAddressType = uk.gov.dluhc.printapi.models.DeliveryAddressType.REGISTERED,
                addressFormat = uk.gov.dluhc.printapi.models.AddressFormat.UK
            )
        )
        processValidRequest(request = request, expectedSupportingFormat = SupportingInformationFormat.STANDARD)
    }

    private fun processValidRequest(
        request: GenerateAnonymousElectorDocumentRequest,
        expectedSupportingFormat: SupportingInformationFormat,
        expectedAddressType: DeliveryAddressType = DeliveryAddressTypeEntity.REGISTERED,
        expectedCollectionReason: String? = null,
    ) {
        // Given
        val localAuthorities: List<LocalAuthorityResponse> = listOf(
            buildLocalAuthorityResponse(
                gssCode = request.gssCode,
            )
        )
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID, localAuthorities = localAuthorities)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByGssCode(eroResponse, request.gssCode)

        val expectedDelivery = with(request.delivery) {
            buildAedDelivery(
                addressee = addressee,
                address = with(deliveryAddress) {
                    buildAddress(
                        street = street,
                        postcode = postcode,
                        property = property,
                        locality = locality,
                        town = town,
                        area = area,
                        uprn = uprn,
                    )
                },
                deliveryClass = DeliveryClass.STANDARD,
                deliveryAddressType = expectedAddressType,
                collectionReason = expectedCollectionReason,
                addressFormat = AddressFormat.UK,
            )
        }

        // When
        val response = webTestClient.mutate()
            .codecs { it.defaultCodecs().maxInMemorySize(MAX_SIZE_2_MB) }
            .build().post()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(request)
            .exchange()
            .expectStatus().isCreated
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            request.gssCode,
            ANONYMOUS_ELECTOR_DOCUMENT,
            request.sourceReference
        )
        assertThat(electorDocuments).hasSize(1)
        val newlyCreatedAed = electorDocuments.first()

        assertThat(newlyCreatedAed)
            .hasId()
            .hasSupportingInformationFormat(expectedSupportingFormat)
            .hasDelivery(expectedDelivery)
            .hasDateCreated()
            .statusHistory {
                it.hasSize(1)
                it.hasStatus(AnonymousElectorDocumentStatus.Status.PRINTED)
            }

        val responseBody = response.responseBody.blockFirst()
        val expectedS3Key =
            "${request.gssCode}/${request.sourceReference}/anonymous-elector-document-${newlyCreatedAed.certificateNumber}.pdf"
        val expectedUrl = matchingPreSignedAwsS3GetUrl(expectedS3Key)
        assertThat(responseBody!!.preSignedUrl).matches {
            it.toString()
                .matches(expectedUrl)
        }

        val aedFromS3 = getObjectFromS3(expectedS3Key)
        val pdfContent = aedFromS3.bytes
        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(newlyCreatedAed.certificateNumber)
            assertThat(text).doesNotContainIgnoringCase(request.surname)
        }

        assertThat(aedFromS3.contentType)
            .isEqualTo(MediaType.APPLICATION_PDF_VALUE)

        assertThat(aedFromS3.tags).isEqualTo(
            listOf(Tag.builder().key("ToDelete").value("True").build())
        )

        await.pollDelay(3, TimeUnit.SECONDS).untilAsserted {
            assertUpdateStatisticsMessageNotSent()
        }
    }

    private fun addPhotoToS3(): String {
        val s3Resource = ResourceUtils.getFile(AED_SAMPLE_PHOTO).readBytes()
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
        val s3Path = "E09000007/0013a30ac9bae2ebb9b1239b/${randomUUID()}/8a53a30ac9bae2ebb9b1239b-test-photo.png"

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
