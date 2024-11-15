package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import uk.gov.dluhc.printapi.config.IntegrationTest.Companion.ERO_ID
import uk.gov.dluhc.printapi.models.CertificateSearchSummaryResponse
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.models.PrintRequestStatus
import uk.gov.dluhc.printapi.models.PrintRequestSummary
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSearchSummaryResults
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildPrintRequestSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aVacPhotoUrl
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
class CertificateSummarySearchResponseMapperTest {

    @Mock
    private lateinit var certificateSummaryResponseMapper: CertificateSummaryResponseMapper

    @InjectMocks
    private lateinit var mapper: CertificateSummarySearchResponseMapperImpl

    @Test
    fun `should map CertificateSearchSummaryResults dto to a CertificateSearchSummaryResponse Api`() {
        // Given
        val expectedOffsetDateTime = OffsetDateTime.now()
        val expectedStatus = PrintRequestStatus.PRINT_MINUS_PROCESSING
        val printRequestDto = buildPrintRequestSummaryDto()
        val certificateSummaryDto = buildCertificateSummaryDto(printRequests = listOf(printRequestDto))
        val results = buildCertificateSearchSummaryResults(
            page = 1,
            pageSize = 50,
            totalPages = 1,
            totalResults = 1,
            results = listOf(certificateSummaryDto),
        )
        val expectedPhotoUrl = aVacPhotoUrl(ERO_ID, certificateSummaryDto.sourceReference)

        val expectedCertificateSummaryResponse = CertificateSummaryResponse(
            vacNumber = certificateSummaryDto.vacNumber,
            applicationReference = certificateSummaryDto.applicationReference,
            sourceReference = certificateSummaryDto.sourceReference,
            firstName = certificateSummaryDto.firstName,
            middleNames = certificateSummaryDto.middleNames,
            surname = certificateSummaryDto.surname,
            photoUrl = expectedPhotoUrl,
            printRequestSummaries = listOf(
                PrintRequestSummary(
                    userId = printRequestDto.userId,
                    dateTime = expectedOffsetDateTime,
                    status = expectedStatus
                )
            )
        )
        given(certificateSummaryResponseMapper.toCertificateSummaryResponse(any(), any())).willReturn(expectedCertificateSummaryResponse)

        val expected = with(results) {
            CertificateSearchSummaryResponse(
                page = page,
                pageSize = pageSize,
                totalResults = totalResults,
                totalPages = totalPages,
                results = listOf(expectedCertificateSummaryResponse)
            )
        }

        // When
        val actual = mapper.toCertificateSearchSummaryResponse(results, ERO_ID)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
