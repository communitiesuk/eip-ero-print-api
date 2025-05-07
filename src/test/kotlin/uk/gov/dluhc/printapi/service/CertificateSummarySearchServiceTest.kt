package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.mapper.CertificateSummaryDtoMapper
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateSpecificationBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSearchSummaryResults
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildCertificateSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificatePageRequest

@ExtendWith(MockitoExtension::class)
internal class CertificateSummarySearchServiceTest {

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Mock
    private lateinit var certificateSummaryDtoMapper: CertificateSummaryDtoMapper

    @Mock
    private lateinit var specificationBuilder: CertificateSpecificationBuilder

    @InjectMocks
    private lateinit var certificateSummarySearchService: CertificateSummarySearchService

    @Test
    fun `should return Voter Authority Certificate summaries`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)

        val expectedPageRequest = buildCertificatePageRequest(page = 1, size = 100)
        val certificate = buildCertificate()
        given(
            certificateRepository.findAll(
                any<Specification<Certificate>>(),
                any<Pageable>()
            )
        ).willReturn(PageImpl(listOf(certificate), expectedPageRequest, 1))

        val expectedSpecification = mock<Specification<Certificate>>()
        given(specificationBuilder.buildSpecification(any(), any())).willReturn(expectedSpecification)

        val expectedSummaryDto = buildCertificateSummaryDto()
        given(certificateSummaryDtoMapper.certificateToCertificatePrintRequestSummaryDto(any())).willReturn(expectedSummaryDto)

        val expected =
            buildCertificateSearchSummaryResults(page = 1, pageSize = 100, results = listOf(expectedSummaryDto))

        val criteria = buildCertificateSearchCriteriaDto(eroId = eroId, page = 1, searchBy = null)

        // When
        val actual = certificateSummarySearchService.searchCertificateSummaries(criteria)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(specificationBuilder).buildSpecification(gssCodes, criteria)
        verify(certificateRepository).findAll(
            expectedSpecification,
            expectedPageRequest
        )
        verify(certificateSummaryDtoMapper).certificateToCertificatePrintRequestSummaryDto(certificate)
    }
}
