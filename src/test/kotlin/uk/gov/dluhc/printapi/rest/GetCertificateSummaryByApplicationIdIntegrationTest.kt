package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.models.PrintRequestStatus
import uk.gov.dluhc.printapi.models.PrintRequestSummary
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.UNAUTHORIZED_BEARER_TOKEN
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.SECONDS

internal class GetCertificateSummaryByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/certificates/applications/{APPLICATION_ID}"
        private const val ERO_ID = "some-city-council"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4aa3"
    }

    @Test
    fun `should return unauthorized given no bearer token`() {
        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should return unauthorized given user with invalid bearer token`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(UNAUTHORIZED_BEARER_TOKEN)
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different group`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
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

        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
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
    fun `should return certificate summary given existing application and user with valid bearer token belonging to same ero and valid group`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID)
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

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
        val status4 = buildPrintRequestStatus(
            status = Status.PRINT_PROVIDER_VALIDATION_FAILED,
            eventDateTime = Instant.now().plusSeconds(3).truncatedTo(SECONDS),
            message = "Print provider production failed"
        )
        val request1 = buildPrintRequest(printRequestStatuses = listOf(status1, status2, status3))
        val request2 = buildPrintRequest(printRequestStatuses = listOf(status4))
        val certificate = buildCertificate(
            gssCode = eroResponse.localAuthorities[0].gssCode!!,
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            printRequests = listOf(request1, request2)
        )

        certificateRepository.save(certificate)
        val expected = CertificateSummaryResponse(
            vacNumber = certificate.vacNumber!!,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
                    userId = request1.userId!!,
                    dateTime = status2.eventDateTime!!.atOffset(ZoneOffset.UTC),
                    message = status2.message
                ),
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_FAILED,
                    userId = request2.userId!!,
                    dateTime = status4.eventDateTime!!.atOffset(ZoneOffset.UTC),
                    message = status4.message
                ),
            )
        )

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()

        // Then
        response.expectStatus().isOk
        val actual = response.returnResult(CertificateSummaryResponse::class.java).responseBody.blockFirst()
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }
}
