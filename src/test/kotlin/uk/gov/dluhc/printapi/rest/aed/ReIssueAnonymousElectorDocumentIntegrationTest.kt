package uk.gov.dluhc.printapi.rest.aed

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.given
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.util.ResourceUtils
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.mapper.aed.AedMappingHelper
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.matchingPreSignedAwsS3GetUrl
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.withBody
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

internal class ReIssueAnonymousElectorDocumentIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents/re-issue"
        private const val GSS_CODE = "W06000099"
        private const val AED_SAMPLE_PHOTO = "classpath:temporary-certificate-template/sample-certificate-photo.png"
        private const val MAX_SIZE_2_MB = 2 * 1024 * 1024

        @JvmStatic
        private fun monthsAndRetentionYears(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(1, 9),
                Arguments.of(2, 9),
                Arguments.of(3, 9),
                Arguments.of(4, 9),
                Arguments.of(5, 9),
                Arguments.of(6, 9),
                Arguments.of(7, 10),
                Arguments.of(8, 10),
                Arguments.of(9, 10),
                Arguments.of(10, 10),
                Arguments.of(11, 10),
                Arguments.of(12, 10),
            )
        }
    }

    @SpyBean
    private lateinit var aedMappingHelper: AedMappingHelper

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

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"
        val previousAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
        )
        anonymousElectorDocumentRepository.save(previousAed)

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )
        assertThat(electorDocuments)
            .hasSize(2) // Exactly 2 AEDs expected in the database for this sourceReference
            .anyMatch { aed ->
                // Of the 2 AEDs in the database the response URL will match only one of them.
                aedMatchesPresignedUrl(aed, presignedUrl)
            }

        val newlyCreatedAed = electorDocuments.first { aed -> // Get the new AED that this test would have created
            aedMatchesPresignedUrl(aed, presignedUrl)
        }
        assertThat(newlyCreatedAed)
            .hasId()
            .statusHistory {
                it.hasSize(1)
                it.hasStatus(AnonymousElectorDocumentStatus.Status.PRINTED)
            }

        val s3Key = "$GSS_CODE/$sourceReference/anonymous-elector-document-${newlyCreatedAed.certificateNumber}.pdf"
        val aedFromS3 = getObjectFromS3(s3Key)
        val pdfContent = aedFromS3.bytes

        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(newlyCreatedAed.certificateNumber)
            assertThat(text).containsIgnoringCase(newElectoralRollNumber)
            assertThat(text).doesNotContain(originalElectoralRollNumber)
        }

        await.pollDelay(3, TimeUnit.SECONDS).untilAsserted {
            assertUpdateApplicationStatisticsMessageNotSent()
        }
    }

    @Test
    fun `should return AED PDF given specified application has multiple previously issued AED, including some with initial data removed`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"

        val oldPreviouslyIssuedAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
        )
        oldPreviouslyIssuedAed.removeInitialRetentionPeriodData()

        val moreRecentAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
        )
        anonymousElectorDocumentRepository.saveAll(listOf(oldPreviouslyIssuedAed, moreRecentAed))

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )
        assertThat(electorDocuments)
            .hasSize(3) // Exactly 3 AEDs expected in the database for this sourceReference (one removed, one previous and one reissue)
            .anyMatch { aed ->
                // Of the 3 AEDs in the database the response URL will match only one of them.
                aedMatchesPresignedUrl(aed, presignedUrl)
            }

        val newlyCreatedAed = electorDocuments.first { aed -> // Get the new AED that this test would have created
            aedMatchesPresignedUrl(aed, presignedUrl)
        }
        assertThat(newlyCreatedAed)
            .hasId()
            .statusHistory {
                it.hasSize(1)
                it.hasStatus(AnonymousElectorDocumentStatus.Status.PRINTED)
            }

        val s3Key = "$GSS_CODE/$sourceReference/anonymous-elector-document-${newlyCreatedAed.certificateNumber}.pdf"
        val aedFromS3 = getObjectFromS3(s3Key)
        val pdfContent = aedFromS3.bytes

        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(newlyCreatedAed.certificateNumber)
            assertThat(text).containsIgnoringCase(newElectoralRollNumber)
            assertThat(text).doesNotContain(originalElectoralRollNumber)
        }

        await.pollDelay(3, TimeUnit.SECONDS).untilAsserted {
            assertUpdateApplicationStatisticsMessageNotSent()
        }
    }

    @Test
    fun `should return AED PDF given specified application has multiple previously issued AED, all with initial data removed`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"

        val oldPreviouslyIssuedAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
        ).also { it.removeInitialRetentionPeriodData() }

        val moreRecentAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
        ).also { it.removeInitialRetentionPeriodData() }

        anonymousElectorDocumentRepository.saveAll(listOf(oldPreviouslyIssuedAed, moreRecentAed))

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )
        assertThat(electorDocuments)
            .hasSize(3) // Exactly 3 AEDs expected in the database for this sourceReference (two removed and one reissue)
            .anyMatch { aed ->
                // Of the 3 AEDs in the database the response PDF filename will match only one of them.
                aedMatchesPresignedUrl(aed, presignedUrl)
            }

        val newlyCreatedAed = electorDocuments.first { aed -> // Get the new AED that this test would have created
            aedMatchesPresignedUrl(aed, presignedUrl)
        }
        assertThat(newlyCreatedAed)
            .hasId()
            .statusHistory {
                it.hasSize(1)
                it.hasStatus(AnonymousElectorDocumentStatus.Status.PRINTED)
            }

        val s3Key = "$GSS_CODE/$sourceReference/anonymous-elector-document-${newlyCreatedAed.certificateNumber}.pdf"
        val aedFromS3 = getObjectFromS3(s3Key)
        val pdfContent = aedFromS3.bytes

        assertThat(pdfContent).isNotNull
        PdfReader(pdfContent).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            assertThat(text).contains(newlyCreatedAed.certificateNumber)
            assertThat(text).containsIgnoringCase(newElectoralRollNumber)
            assertThat(text).doesNotContain(originalElectoralRollNumber)
        }

        await.pollDelay(3, TimeUnit.SECONDS).untilAsserted {
            assertUpdateApplicationStatisticsMessageNotSent()
        }
    }

    @Test
    fun `should set initial data retention date for new AED given previously issued AED has initial data retention date set`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()
        val originalRetentionRemovalDate = LocalDate.now().minusMonths(4)

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"
        val previousAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
            initialRetentionRemovalDate = originalRetentionRemovalDate,
        )
        anonymousElectorDocumentRepository.save(previousAed)

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )

        // Get the new AED that this test would have created
        val newlyCreatedAed = electorDocuments.first { aed ->
            aedMatchesPresignedUrl(aed, presignedUrl)
        }

        val expectedNewRetentionDate = LocalDate.now().plusMonths(15)
        assertThat(newlyCreatedAed)
            .hasInitialRetentionRemovalDate(expectedNewRetentionDate)
    }

    @Test
    fun `should not set initial data retention date for new AED given previously issued AED does not have the initial data retention date set`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"
        val previousAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
            initialRetentionRemovalDate = null,
        )
        anonymousElectorDocumentRepository.save(previousAed)

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )

        // Get the new AED that this test would have created
        val newlyCreatedAed = electorDocuments.first { aed ->
            aedMatchesPresignedUrl(aed, presignedUrl)
        }

        assertThat(newlyCreatedAed).hasInitialRetentionRemovalDate(null)
    }

    @ParameterizedTest
    @MethodSource("monthsAndRetentionYears")
    fun `should set final data retention date for new AED given previously issued AED has final data retention date set`(
        month: Int,
        retentionYears: Int
    ) {
        // Given

        // The value of the final retention date depends on what month the certificate is issued/re-issued.
        // We're effectively mocking the date of "today" and check that the result is correct for all months.
        val issueDate = LocalDate.now()
            .withMonth(month)
            .withDayOfMonth(1)
        given(aedMappingHelper.issueDate()).willReturn(issueDate)

        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()
        val originalRetentionRemovalDate = LocalDate.now().minusMonths(4)

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"
        val previousAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
            finalRetentionRemovalDate = originalRetentionRemovalDate,
        )
        anonymousElectorDocumentRepository.save(previousAed)

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )

        // Get the new AED that this test would have created
        val newlyCreatedAed = electorDocuments.first { aed ->
            aedMatchesPresignedUrl(aed, presignedUrl)
        }

        val expectedNewRetentionDate = LocalDate.of(issueDate.year + retentionYears, 7, 1)
        assertThat(newlyCreatedAed).hasFinalRetentionRemovalDate(expectedNewRetentionDate)
    }

    @Test
    fun `should not set final data retention date for new AED given previously issued AED does not have the final data retention date set`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val sourceReference = aValidSourceReference()
        val photoLocationArn = addPhotoToS3()

        val originalElectoralRollNumber = "ORIGINAL ELECTORAL ROLL #"
        val previousAed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = sourceReference,
            photoLocationArn = photoLocationArn,
            electoralRollNumber = originalElectoralRollNumber,
            finalRetentionRemovalDate = null,
        )
        anonymousElectorDocumentRepository.save(previousAed)

        val newElectoralRollNumber = aValidElectoralRollNumber()
        val requestBody = buildReIssueAnonymousElectorDocumentRequest(
            sourceReference = sourceReference,
            electoralRollNumber = newElectoralRollNumber,
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
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val responseBody = response.responseBody.blockFirst()
        val presignedUrl = responseBody!!.preSignedUrl

        val electorDocuments = anonymousElectorDocumentRepository.findByGssCodeAndSourceTypeAndSourceReference(
            GSS_CODE,
            ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference
        )

        // Get the new AED that this test would have created
        val newlyCreatedAed = electorDocuments.first { aed ->
            aedMatchesPresignedUrl(aed, presignedUrl)
        }

        assertThat(newlyCreatedAed).hasFinalRetentionRemovalDate(null)
    }

    private fun addPhotoToS3(): String {
        val s3Resource = ResourceUtils.getFile(AED_SAMPLE_PHOTO).readBytes()
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

    private fun aedMatchesPresignedUrl(aed: uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument, presignedUrl: URI): Boolean {
        val s3Key = "${aed.gssCode}/${aed.sourceReference}/anonymous-elector-document-${aed.certificateNumber}.pdf"
        val expectedUrl = matchingPreSignedAwsS3GetUrl(s3Key)
        return presignedUrl.toString().matches(expectedUrl)
    }
}
