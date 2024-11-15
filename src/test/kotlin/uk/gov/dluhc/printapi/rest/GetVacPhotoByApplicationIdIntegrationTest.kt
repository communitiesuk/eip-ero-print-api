package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.testsupport.addCertificatePhotoToS3
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.buildS3Arn
import uk.gov.dluhc.printapi.testsupport.matchingPreSignedAwsS3GetUrl
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucketPath

internal class GetVacPhotoByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE =
            "/eros/{ERO_ID}/certificates/photo?applicationId={APPLICATION_ID}"
        private const val APPLICATION_ID = "6407b6158f529a11713a1e5c"
        private const val GSS_CODE = "W06000099"
    }

    @Test
    fun `should return bad request given request without applicationId query string parameter`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri("/eros/{ERO_ID}/certificates/photo", ERO_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return not found given no VAC exists for application ID`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("Certificate for eroId = $ERO_ID with sourceType = VOTER_CARD and sourceReference = $APPLICATION_ID not found")
    }

    @Test
    fun `should return a presigned url for the VAC photo`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
        val s3Path = aPhotoBucketPath()
        val certificate = buildCertificate(
            gssCode = eroResponse.localAuthorities[0].gssCode,
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            photoLocationArn = buildS3Arn(s3Bucket, s3Path)
        )
        certificateRepository.save(certificate)
        s3Client.addCertificatePhotoToS3(s3Bucket, s3Path)

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        val expectedUrl = matchingPreSignedAwsS3GetUrl(s3Path)
        assertThat(actual!!.preSignedUrl).matches {
            it.toString()
                .matches(expectedUrl)
        }
    }

    @Test
    fun `should return a presigned url for the VAC photo given object key contains special characters`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
        val photoObjectKey = "dir1/Jane+!@£$%^&*())))_+-=[]{}'\\|;;<>,.:?#`~§± Doe:Awesome Company Ltd:HEADSHOT.jpg"
        val certificate = buildCertificate(
            gssCode = eroResponse.localAuthorities[0].gssCode,
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            photoLocationArn = buildS3Arn(s3Bucket, photoObjectKey)
        )
        certificateRepository.save(certificate)
        s3Client.addCertificatePhotoToS3(s3Bucket, photoObjectKey)
        val expectedEncodedPath =
            "dir1/Jane%2B%21%40%C2%A3%24%25%5E%26%2A%28%29%29%29%29_%2B-%3D%5B%5D%7B%7D%27%5C%7C%3B%3B%3C%3E%2C.%3A%3F%23%60~%C2%A7%C2%B1%20Doe%3AAwesome%20Company%20Ltd%3AHEADSHOT.jpg"

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .returnResult(PreSignedUrlResourceResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual!!.preSignedUrl.rawPath).isEqualTo("/$s3Bucket/$expectedEncodedPath")
    }
}
