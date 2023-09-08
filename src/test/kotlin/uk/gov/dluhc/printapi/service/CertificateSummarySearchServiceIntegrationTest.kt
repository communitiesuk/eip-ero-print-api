package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.dto.CertificateSearchBy
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import java.time.LocalDate

internal class CertificateSummarySearchServiceIntegrationTest : IntegrationTest() {

    companion object {
        private const val GSS_CODE = "W06000099"
        private const val ANOTHER_GSS_CODE = "E06000123"
    }

    @Test
    fun `should return all VACs for gsscode given no search by criteria`() {
        // Given
        createAndSaveSomeVacs()

        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val criteria = buildCertificateSearchCriteriaDto(
            eroId = ERO_ID,
            page = 1,
            pageSize = 100,
            searchBy = null,
            searchValue = null
        )

        // When
        val actual = certificateSummarySearchService.searchCertificateSummaries(criteria)

        // Then
        assertThat(actual)
            .isPage(1)
            .hasPageSize(100)
            .hasTotalPages(1)
            .hasTotalResults(4)
            .resultsAreForApplicationReferences(
                "APP_REF_3", // VAC3 first because it's the most recent (today)
                "APP_REF_1", // VAC1 2nd because it has the same date as VAC2 but the reference is APP_REF_1 (9 days old)
                "APP_REF_2", // VAC2 3rd because it has the same date as VAC1 but the reference is APP_REF_2 (9 days old)
                "APP_REF_4", // VAC4 last because it's the oldest (10 days old)
            )
    }
    @Test
    fun `should return all VACs for gsscode given application_reference search by criteria`() {
        // Given
        createAndSaveSomeVacs()

        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val criteria = buildCertificateSearchCriteriaDto(
            eroId = ERO_ID,
            page = 1,
            pageSize = 100,
            searchBy = CertificateSearchBy.APPLICATION_REFERENCE,
            searchValue = "APP_REF_3"
        )

        // When
        val actual = certificateSummarySearchService.searchCertificateSummaries(criteria)

        // Then
        assertThat(actual)
            .isPage(1)
            .hasPageSize(100)
            .hasTotalPages(1)
            .hasTotalResults(1)
            .resultsAreForApplicationReferences(
                "APP_REF_3", // VAC3 because it has the application reference APP_REF_3
            )
    }

    @Test
    fun `should return no VACs for gsscode given search by criteria that matches nothing`() {
        // Given
        createAndSaveSomeVacs()

        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val criteria = buildCertificateSearchCriteriaDto(
            eroId = ERO_ID,
            page = 1,
            pageSize = 100,
            searchBy = CertificateSearchBy.APPLICATION_REFERENCE,
            searchValue = "ABCDEFG"
        )

        // When
        val actual = certificateSummarySearchService.searchCertificateSummaries(criteria)

        // Then
        assertThat(actual)
            .isPage(0)
            .hasPageSize(100)
            .hasTotalPages(0)
            .hasTotalResults(0)
            .hasResults(emptyList())
    }

    private fun createAndSaveSomeVacs() {
        val certificate1Ref = "APP_REF_1"
        val certificate1 = buildCertificate(
            gssCode = GSS_CODE,
            sourceType = SourceType.VOTER_CARD,
            applicationReference = certificate1Ref,
            issueDate = LocalDate.now().minusDays(9)
        )

        val certificate2Ref = "APP_REF_2"
        val certificate2 = buildCertificate(
            gssCode = GSS_CODE,
            sourceType = SourceType.VOTER_CARD,
            applicationReference = certificate2Ref,
            issueDate = LocalDate.now().minusDays(9)
        )

        val certificate3Ref = "APP_REF_3"
        val certificate3 = buildCertificate(
            gssCode = GSS_CODE,
            sourceType = SourceType.VOTER_CARD,
            applicationReference = certificate3Ref,
            issueDate = LocalDate.now()
        )

        val certificate4Ref = "APP_REF_4"
        val certificate4 = buildCertificate(
            gssCode = GSS_CODE,
            sourceType = SourceType.VOTER_CARD,
            applicationReference = certificate4Ref,
            issueDate = LocalDate.now().minusDays(10)
        )

        val certificate5Ref = "APP_REF_5"
        val certificate5 = buildCertificate(
            gssCode = ANOTHER_GSS_CODE,
            sourceType = SourceType.VOTER_CARD,
            applicationReference = certificate5Ref
        )

        certificateRepository.saveAll(
            listOf(
                certificate1,
                certificate2,
                certificate3,
                certificate4,
                certificate5
            )
        )
    }
}
