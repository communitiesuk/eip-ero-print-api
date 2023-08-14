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
import uk.gov.dluhc.printapi.database.mapper.VacSummaryDtoMapper
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateSpecificationBuilder
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSearchSummaryResults
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildVacSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildVacPageRequest

@ExtendWith(MockitoExtension::class)
internal class VacSummarySearchServiceTest {

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var certificateRepository: CertificateRepository

    @Mock
    private lateinit var vacSummaryDtoMapper: VacSummaryDtoMapper

    @Mock
    private lateinit var specificationBuilder: CertificateSpecificationBuilder

    @InjectMocks
    private lateinit var vacSummarySearchService: VacSummarySearchService

    @Test
    fun `should return Voter Authority Certificate summaries`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)

        val expectedPageRequest = buildVacPageRequest(page = 1, size = 100)
        val certificate = buildCertificate()
        given(
            certificateRepository.findAll(
                any<Specification<Certificate>>(),
                any<Pageable>()
            )
        ).willReturn(PageImpl(listOf(certificate), expectedPageRequest, 1))

        val expectedSpecification = mock<Specification<Certificate>>()
        given(specificationBuilder.buildSpecification(any(), any())).willReturn(expectedSpecification)

        val expectedSummaryDto = buildVacSummaryDto()
        given(vacSummaryDtoMapper.certificateToVacSummaryDto(any())).willReturn(expectedSummaryDto)

        val expected =
            buildVacSearchSummaryResults(page = 1, pageSize = 100, results = listOf(expectedSummaryDto))

        val criteria = buildVacSearchCriteriaDto(eroId = eroId, page = 1, searchBy = null)

        // When
        val actual = vacSummarySearchService.searchVacSummaries(criteria)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(specificationBuilder).buildSpecification(gssCodes, criteria)
        verify(certificateRepository).findAll(
            expectedSpecification,
            expectedPageRequest
        )
        verify(vacSummaryDtoMapper).certificateToVacSummaryDto(certificate)
    }
}
