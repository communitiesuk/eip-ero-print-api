package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.util.UriComponentsBuilder
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.models.CertificateSearchBy
import uk.gov.dluhc.printapi.models.CertificateSearchBy.APPLICATION_REFERENCE
import uk.gov.dluhc.printapi.models.CertificateSearchBy.SURNAME
import uk.gov.dluhc.printapi.models.CertificateSearchSummaryResponse
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.models.PrintRequestStatus
import uk.gov.dluhc.printapi.models.PrintRequestSummary
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import uk.gov.dluhc.printapi.models.DeliveryAddressType as DeliveryAddressTypeResponse

internal class SearchCertificateSummariesIntegrationTest : IntegrationTest() {
    companion object {
        private const val SEARCH_SUMMARY_URI_TEMPLATE = "/eros/{eroId}/certificates/search"
        private const val GSS_CODE = "W06000099"
        private const val ANOTHER_GSS_CODE = "E06000123"
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()

        // When
        webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, searchBy = APPLICATION_REFERENCE))
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `should return empty list given no VACs exist for an ERO`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        // When
        val response = webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(CertificateSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        with(actual!!) {
            assertThat(page).isZero()
            assertThat(pageSize).isEqualTo(100)
            assertThat(totalPages).isZero()
            assertThat(totalResults).isZero()
            assertThat(results).isNotNull.isEmpty()
        }
    }

    @Test
    fun `should return all summaries for an ero given no pagination and no searchBy specified`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val currentDate = LocalDate.now()
        val vac1SourceReference = aValidSourceReference()
        val vac1ApplicationReference = aValidApplicationReference()
        val vac1Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac1Request = buildPrintRequest(
            printRequestStatuses = listOf(vac1Status),
            delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.ERO_COLLECTION)
        )
        val vac1 = buildCertificate(
            gssCode = GSS_CODE,
            sourceReference = vac1SourceReference,
            applicationReference = vac1ApplicationReference,
            issueDate = currentDate.minusDays(5),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac1Request)
        )

        val vac2SourceReference = aValidSourceReference()
        val vac2ApplicationReference = aValidApplicationReference()
        val vac2Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac2Request = buildPrintRequest(
            printRequestStatuses = listOf(vac2Status),
            delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
        )
        val vac2 = buildCertificate(
            gssCode = GSS_CODE,
            sourceReference = vac2SourceReference,
            applicationReference = vac2ApplicationReference,
            issueDate = currentDate.minusDays(10),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac2Request)
        )

        val vac3SourceReference = aValidSourceReference()
        val vac3ApplicationReference = aValidApplicationReference()
        val vac3Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac3Request = buildPrintRequest(printRequestStatuses = listOf(vac3Status))
        val vac3 = buildCertificate(
            gssCode = ANOTHER_GSS_CODE,
            sourceReference = vac3SourceReference,
            applicationReference = vac3ApplicationReference,
            issueDate = currentDate.minusDays(10),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac3Request)
        )

        certificateRepository.saveAll(
            listOf(
                vac1,
                vac2,
                vac3
            )
        )

        val expectedResult1 = CertificateSummaryResponse(
            vacNumber = vac1.vacNumber!!,
            applicationReference = vac1ApplicationReference,
            sourceReference = vac1SourceReference,
            firstName = vac1Request.firstName,
            middleNames = vac1Request.middleNames,
            surname = vac1Request.surname,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
                    userId = vac1Request.userId!!,
                    dateTime = vac1Request.requestDateTime!!.atOffset(ZoneOffset.UTC),
                    message = vac1Status.message,
                    deliveryAddressType = DeliveryAddressTypeResponse.ERO_MINUS_COLLECTION
                )
            )
        )
        val expectedResult2 = CertificateSummaryResponse(
            vacNumber = vac2.vacNumber!!,
            applicationReference = vac2ApplicationReference,
            sourceReference = vac2SourceReference,
            firstName = vac2Request.firstName,
            middleNames = vac2Request.middleNames,
            surname = vac2Request.surname,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
                    userId = vac2Request.userId!!,
                    dateTime = vac2Request.requestDateTime!!.atOffset(ZoneOffset.UTC),
                    message = vac2Status.message,
                    deliveryAddressType = DeliveryAddressTypeResponse.REGISTERED
                ),
            )
        )
        val expectedResultsInExactOrder = listOf(expectedResult1, expectedResult2)
        val expectedTotalPages = 1
        val expectedTotalResults = 2

        // When
        val response = webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(CertificateSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        with(actual!!) {
            assertThat(page).isEqualTo(1)
            assertThat(pageSize).isEqualTo(100)
            assertThat(totalPages).isEqualTo(expectedTotalPages)
            assertThat(totalResults).isEqualTo(expectedTotalResults)
            assertThat(results).isNotEmpty
                .hasSize(2)
                .usingRecursiveComparison()
                .isEqualTo(expectedResultsInExactOrder)
        }
    }

    @Test
    fun `should return summaries that have had delivery information removed`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val currentDate = LocalDate.now()
        val vacSourceReference = aValidSourceReference()
        val vacApplicationReference = aValidApplicationReference()
        val vacStatus = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vacRequest = buildPrintRequest(
            printRequestStatuses = listOf(vacStatus),
            delivery = null,
            supportingInformationFormat = null,
        )
        val vac = buildCertificate(
            gssCode = GSS_CODE,
            sourceReference = vacSourceReference,
            applicationReference = vacApplicationReference,
            issueDate = currentDate.minusDays(5),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vacRequest),
            initialRetentionDataRemoved = true,
        )

        certificateRepository.save(vac)

        val expectedResult = CertificateSummaryResponse(
            vacNumber = vac.vacNumber!!,
            applicationReference = vacApplicationReference,
            sourceReference = vacSourceReference,
            firstName = vacRequest.firstName,
            middleNames = vacRequest.middleNames,
            surname = vacRequest.surname,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
                    userId = vacRequest.userId!!,
                    dateTime = vacRequest.requestDateTime!!.atOffset(ZoneOffset.UTC),
                    message = vacStatus.message,
                    deliveryAddressType = null
                )
            )
        )

        // When
        val response = webTestClient.get()
            .uri(buildUri(uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE, eroId = ERO_ID))
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(CertificateSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        with(actual!!) {
            assertThat(results).isNotEmpty
                .hasSize(1)
                .usingRecursiveComparison()
                .isEqualTo(listOf(expectedResult))
        }
    }

    @Test
    fun `should return summaries matching application reference given searchBy applicationReference specified`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val currentDate = LocalDate.now()

        val vac1SourceReference = aValidSourceReference()
        val vac1ApplicationReference = aValidApplicationReference()
        val vac1Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac1Request = buildPrintRequest(
            printRequestStatuses = listOf(vac1Status),
            delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
        )
        val vac1 = buildCertificate(
            gssCode = GSS_CODE,
            sourceReference = vac1SourceReference,
            applicationReference = vac1ApplicationReference,
            issueDate = currentDate.minusDays(5),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac1Request)
        )

        val vac2SourceReference = aValidSourceReference()
        val vac2ApplicationReference = aValidApplicationReference()
        val vac2Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac2Request = buildPrintRequest(printRequestStatuses = listOf(vac2Status))
        val vac2 = buildCertificate(
            gssCode = GSS_CODE,
            sourceReference = vac2SourceReference,
            applicationReference = vac2ApplicationReference,
            issueDate = currentDate.minusDays(10),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac2Request)
        )

        certificateRepository.saveAll(
            listOf(
                vac1,
                vac2
            )
        )

        val expectedResult1 = CertificateSummaryResponse(
            vacNumber = vac1.vacNumber!!,
            applicationReference = vac1ApplicationReference,
            sourceReference = vac1SourceReference,
            firstName = vac1Request.firstName,
            middleNames = vac1Request.middleNames,
            surname = vac1Request.surname,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
                    userId = vac1Request.userId!!,
                    dateTime = vac1Status.eventDateTime!!.atOffset(ZoneOffset.UTC),
                    message = vac1Status.message,
                    deliveryAddressType = DeliveryAddressTypeResponse.REGISTERED
                )
            )
        )
        val expectedResults = listOf(expectedResult1)

        // When
        val response = webTestClient.get()
            .uri(
                buildUri(
                    uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE,
                    eroId = ERO_ID,
                    page = 1,
                    pageSize = 100,
                    searchBy = APPLICATION_REFERENCE,
                    searchValue = vac1ApplicationReference
                )
            )
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(CertificateSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual!!.results).isNotEmpty
            .hasSize(1)
            .usingRecursiveComparison()
            .isEqualTo(expectedResults)
    }

    @Test
    fun `should return summaries with print requests matching surname given searchBy surname specified`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val currentDate = LocalDate.now()

        val vac1Surname = aValidSurname()
        val vac1Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac1Request = buildPrintRequest(
            surname = vac1Surname,
            printRequestStatuses = listOf(vac1Status),
            delivery = buildDelivery(deliveryAddressType = DeliveryAddressType.REGISTERED)
        )
        val vac1 = buildCertificate(
            gssCode = GSS_CODE,
            issueDate = currentDate.minusDays(5),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac1Request)
        )

        val vac2Surname = aValidSurname()
        val vac2Status = buildPrintRequestStatus(
            status = uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH,
            eventDateTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )
        val vac2Request = buildPrintRequest(surname = vac2Surname, printRequestStatuses = listOf(vac2Status))
        val vac2 = buildCertificate(
            gssCode = GSS_CODE,
            issueDate = currentDate.minusDays(10),
            sourceType = SourceType.VOTER_CARD,
            printRequests = listOf(vac2Request)
        )

        certificateRepository.saveAll(
            listOf(
                vac1,
                vac2
            )
        )

        val expectedResult1 = CertificateSummaryResponse(
            vacNumber = vac1.vacNumber!!,
            applicationReference = vac1.applicationReference!!,
            sourceReference = vac1.sourceReference!!,
            firstName = vac1Request.firstName,
            middleNames = vac1Request.middleNames,
            surname = vac1Request.surname,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    status = PrintRequestStatus.PRINT_MINUS_PROCESSING,
                    userId = vac1Request.userId!!,
                    dateTime = vac1Status.eventDateTime!!.atOffset(ZoneOffset.UTC),
                    message = vac1Status.message,
                    deliveryAddressType = DeliveryAddressTypeResponse.REGISTERED
                )
            )
        )
        val expectedResults = listOf(expectedResult1)

        // When
        val response = webTestClient.get()
            .uri(
                buildUri(
                    uriTemplate = SEARCH_SUMMARY_URI_TEMPLATE,
                    eroId = ERO_ID,
                    page = 1,
                    pageSize = 100,
                    searchBy = SURNAME,
                    searchValue = vac1Surname
                )
            )
            .bearerToken(getVCAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(CertificateSearchSummaryResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual!!.results).isNotEmpty
            .hasSize(1)
            .usingRecursiveComparison()
            .isEqualTo(expectedResults)
    }

    private fun buildUri(
        uriTemplate: String,
        uriBuilder: UriComponentsBuilder = UriComponentsBuilder.fromUriString(uriTemplate),
        eroId: String = ERO_ID,
        page: Int? = null,
        pageSize: Int? = null,
        searchBy: CertificateSearchBy? = null,
        searchValue: String? = null,
    ): String {
        if (page != null) {
            uriBuilder.queryParam("page", page)
        }
        if (pageSize != null) {
            uriBuilder.queryParam("pageSize", pageSize)
        }
        if (searchBy != null) {
            uriBuilder.queryParam("searchBy", searchBy.value)
        }
        if (searchValue != null) {
            uriBuilder.queryParam("searchValue", searchValue)
        }

        return uriBuilder.buildAndExpand(mapOf("eroId" to eroId)).toUriString()
    }
}
