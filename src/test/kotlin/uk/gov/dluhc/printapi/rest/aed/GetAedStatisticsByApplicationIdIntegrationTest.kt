package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.models.CertificateStatisticsStatus
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.models.StatisticsResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken

internal class GetAedStatisticsByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/anonymous-elector-documents/statistics?applicationId={APPLICATION_ID}"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4bbc"
    }

    @Test
    fun `should return bad request given request without applicationId query string parameter`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri("/anonymous-elector-documents/statistics", ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `should return statistics for an AED that been issued once`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()

        val aed = buildAnonymousElectorDocument(sourceReference = APPLICATION_ID)
        anonymousElectorDocumentRepository.save(aed)

        val expectedResponse = StatisticsResponse(
            certificateStatus = CertificateStatisticsStatus.DISPATCHED,
            certificateReprinted = false,
            temporaryCertificateIssued = false,
        )

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(StatisticsResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }

    @Test
    fun `should return statistics for an AED that has been issued multiple times`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()

        val aed1 = buildAnonymousElectorDocument(sourceReference = APPLICATION_ID)
        val aed2 = buildAnonymousElectorDocument(sourceReference = APPLICATION_ID)
        anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2))

        val expectedResponse = StatisticsResponse(
            certificateStatus = CertificateStatisticsStatus.DISPATCHED,
            certificateReprinted = true,
            temporaryCertificateIssued = false,
        )

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(StatisticsResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }

    @Test
    fun `should return not found given no AEDs exist for application ID`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()

        // Then
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .exchange()
            .expectStatus().isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("Certificate with sourceType = ANONYMOUS_ELECTOR_DOCUMENT and sourceReference = $APPLICATION_ID not found")
    }
}
