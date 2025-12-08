package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.NOT_FOUND
import uk.gov.dluhc.internalauth.Constants
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.models.CertificateStatisticsStatus
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.models.StatisticsResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import java.time.Instant
import java.time.temporal.ChronoUnit.SECONDS

internal class GetVacStatisticsByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/certificates/statistics?applicationId={APPLICATION_ID}"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4aa3"
    }

    @BeforeEach
    fun resetWiremock() {
        wireMockService.resetAllStubsAndMappings()
    }

    @Test
    fun `should return bad request given request without applicationId query string parameter`() {
        webTestClient.get()
            .uri("/certificates/statistics")
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `should return unauthorised given no auth header present`() {
        // When / Then
        webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `should return unauthorised given failed sts auth request`() {
        // Given
        wireMockService.stubUnsuccessfulStsPresignedAuthResponse()

        // When / Then
        webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .header(Constants.AUTH_HEADER_NAME, "query=something")
            .exchange()
            .expectStatus()
            .isUnauthorized()
    }

    @Test
    fun `should return forbidden given role is not allowed to access statistics`() {
        // Given
        wireMockService.stubSuccessfulStsPresignedAuthResponse("wrong-role")

        // When / Then
        webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .header(Constants.AUTH_HEADER_NAME, "query=something")
            .exchange()
            .expectStatus()
            .isForbidden()
    }

    @Test
    fun `should return not found given application does not exist`() {
        // Given
        wireMockService.stubSuccessfulStsPresignedAuthResponse("stats-role")

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .header(Constants.AUTH_HEADER_NAME, "query=something")
            .exchange()
            .expectStatus()
            .isEqualTo(NOT_FOUND)
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual)
            .hasStatus(NOT_FOUND.value())
            .hasError("Not Found")
            .hasMessage("Certificate with sourceType = ${SourceType.VOTER_CARD} and sourceReference = $APPLICATION_ID not found")
    }

    @Test
    fun `should return statistics for a VAC that been issued once with no temporary certificates`() {
        // Given
        val status1 = buildPrintRequestStatus(
            status = Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().plusSeconds(2).truncatedTo(SECONDS)
        )
        val status2 = buildPrintRequestStatus(
            status = Status.SENT_TO_PRINT_PROVIDER,
            eventDateTime = Instant.now().plusSeconds(4).truncatedTo(SECONDS),
            message = "Sent to print provider"
        )
        val status3 = buildPrintRequestStatus(
            status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(SECONDS),
        )
        val request = buildPrintRequest(
            printRequestStatuses = listOf(status1, status2, status3),
            requestDateTime = Instant.now().truncatedTo(SECONDS)
        )
        val certificate = buildCertificate(
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            printRequests = listOf(request),
            status = Status.SENT_TO_PRINT_PROVIDER
        )

        certificateRepository.save(certificate)
        val expectedResponse = StatisticsResponse(
            certificateStatus = CertificateStatisticsStatus.SENT_TO_PRINT_PROVIDER,
            certificateReprinted = false,
            temporaryCertificateIssued = false,
        )

        wireMockService.stubSuccessfulStsPresignedAuthResponse("other-stats-role")

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .header(Constants.AUTH_HEADER_NAME, "query=something")
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
    fun `should return statistics for a VAC with multiple print requests`() {
        // Given
        val status1 = buildPrintRequestStatus(status = Status.PRINT_PROVIDER_VALIDATION_FAILED)
        val request1 = buildPrintRequest(
            printRequestStatuses = listOf(status1),
            requestDateTime = Instant.now().truncatedTo(SECONDS)
        )
        val status2 = buildPrintRequestStatus(status = Status.SENT_TO_PRINT_PROVIDER)
        val request2 = buildPrintRequest(
            printRequestStatuses = listOf(status2),
            requestDateTime = Instant.now().plusSeconds(4).truncatedTo(SECONDS)
        )
        val certificate = buildCertificate(
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            printRequests = listOf(request1, request2),
            status = Status.SENT_TO_PRINT_PROVIDER,
        )

        certificateRepository.save(certificate)
        val expectedResponse = StatisticsResponse(
            certificateStatus = CertificateStatisticsStatus.SENT_TO_PRINT_PROVIDER,
            certificateReprinted = true,
            temporaryCertificateIssued = false,
        )

        wireMockService.stubSuccessfulStsPresignedAuthResponse("stats-role")

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .header(Constants.AUTH_HEADER_NAME, "query=something")
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
    fun `should return statistics for a VAC with temporary certificates issued`() {
        // Given
        val status = buildPrintRequestStatus(status = Status.SENT_TO_PRINT_PROVIDER)
        val request = buildPrintRequest(
            printRequestStatuses = listOf(status),
            requestDateTime = Instant.now().truncatedTo(SECONDS)
        )
        val certificate = buildCertificate(
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            printRequests = listOf(request),
            status = Status.SENT_TO_PRINT_PROVIDER
        )

        certificateRepository.save(certificate)

        val temporaryCertificate = buildTemporaryCertificate(
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
        )

        temporaryCertificateRepository.save(temporaryCertificate)

        val expectedResponse = StatisticsResponse(
            certificateStatus = CertificateStatisticsStatus.SENT_TO_PRINT_PROVIDER,
            certificateReprinted = false,
            temporaryCertificateIssued = true,
        )

        wireMockService.stubSuccessfulStsPresignedAuthResponse("other-stats-role")

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, APPLICATION_ID)
            .header(Constants.AUTH_HEADER_NAME, "query=something")
            .exchange()
            .expectStatus().isOk
            .returnResult(StatisticsResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
            .usingRecursiveComparison()
            .isEqualTo(expectedResponse)
    }
}
