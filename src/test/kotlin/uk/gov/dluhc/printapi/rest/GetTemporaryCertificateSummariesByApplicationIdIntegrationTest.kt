package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.models.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.models.TemporaryCertificateSummariesResponse
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildTemporaryCertificateSummary
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.SECONDS

internal class GetTemporaryCertificateSummariesByApplicationIdIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/temporary-certificates?applicationId={APPLICATION_ID}"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4aa3"
    }

    @Test
    fun `should return bad request given request without applicationId query string parameter`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri("/eros/{ERO_ID}/temporary-certificates", ERO_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(MediaType.APPLICATION_JSON)
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
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return Temporary Certificate Summaries`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID)
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val temporaryCertificate1 = buildTemporaryCertificate(
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            gssCode = eroResponse.localAuthorities[0].gssCode,
        ).also {
            temporaryCertificateRepository.save(it)
        }

        Thread.sleep(1000) // sleep for 1 second to ensure that the 2 temp certs are saved at different times and we can therefore predict and assert the order

        val temporaryCertificate2 = buildTemporaryCertificate(
            sourceType = SourceType.VOTER_CARD,
            sourceReference = APPLICATION_ID,
            gssCode = eroResponse.localAuthorities[0].gssCode,
        ).also {
            temporaryCertificateRepository.save(it)
        }

        val expectedTemporaryCertificateSummary1 = with(temporaryCertificate1) {
            buildTemporaryCertificateSummary(
                certificateNumber = certificateNumber!!,
                status = TemporaryCertificateStatus.GENERATED,
                userId = userId!!,
                dateTimeGenerated = dateTimeGenerated!!.atOffset(UTC).truncatedTo(SECONDS),
                issueDate = issueDate,
                validOnDate = validOnDate!!,
            )
        }
        val expectedTemporaryCertificateSummary2 = with(temporaryCertificate2) {
            buildTemporaryCertificateSummary(
                certificateNumber = certificateNumber!!,
                status = TemporaryCertificateStatus.GENERATED,
                userId = userId!!,
                dateTimeGenerated = dateTimeGenerated!!.atOffset(UTC).truncatedTo(SECONDS),
                issueDate = issueDate,
                validOnDate = validOnDate!!,
            )
        }

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk

        // Then
        val actual = response.returnResult(TemporaryCertificateSummariesResponse::class.java).responseBody.blockFirst()
        with(actual!!) {
            assertThat(temporaryCertificates).hasSize(2)
            // Assert that temp cert 2 is first in the list because it has a later dateTimeGenerated
            // Ignore field comparison of dateTimeGenerated because millisecond rounding is hard to predict and control
            assertThat(temporaryCertificates[0])
                .usingRecursiveComparison()
                .ignoringFields("dateTimeGenerated")
                .isEqualTo(expectedTemporaryCertificateSummary2)
            assertThat(temporaryCertificates[1])
                .usingRecursiveComparison()
                .ignoringFields("dateTimeGenerated")
                .isEqualTo(expectedTemporaryCertificateSummary1)
        }
    }
}
