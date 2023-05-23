package uk.gov.dluhc.printapi.service.aed

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
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummarySpecificationBuilder
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryResults
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryEntity
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPageRequest

@ExtendWith(MockitoExtension::class)
internal class AnonymousElectorDocumentSearchServiceTest {

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var anonymousElectorDocumentSummaryRepository: AnonymousElectorDocumentSummaryRepository

    @Mock
    private lateinit var anonymousSearchSummaryMapper: AnonymousSearchSummaryMapper

    @Mock
    private lateinit var specificationBuilder: AnonymousElectorDocumentSummarySpecificationBuilder

    @InjectMocks
    private lateinit var anonymousElectorDocumentSearchService: AnonymousElectorDocumentSearchService

    @Test
    fun `should return Anonymous Elector Document summaries`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)

        val expectedPageRequest = buildPageRequest(page = 1, size = 100)
        val aedSummary = buildAnonymousElectorDocumentSummaryEntity()
        given(
            anonymousElectorDocumentSummaryRepository.findAll(
                any<Specification<AnonymousElectorDocumentSummary>>(),
                any<Pageable>()
            )
        ).willReturn(PageImpl(listOf(aedSummary), expectedPageRequest, 1))

        val expectedSpecification = mock<Specification<AnonymousElectorDocumentSummary>>()
        given(specificationBuilder.buildSpecification(any(), any())).willReturn(expectedSpecification)

        val expectedSummaryDto = buildAnonymousSearchSummaryDto()
        given(anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(any())).willReturn(expectedSummaryDto)

        val expected =
            buildAnonymousSearchSummaryResults(page = 1, pageSize = 100, results = listOf(expectedSummaryDto))

        val criteria = buildAnonymousSearchCriteriaDto(eroId = eroId, page = 1, searchBy = null)

        // When
        val actual = anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(criteria)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(specificationBuilder).buildSpecification(gssCodes, criteria)
        verify(anonymousElectorDocumentSummaryRepository).findAll(
            expectedSpecification,
            expectedPageRequest
        )
        verify(anonymousSearchSummaryMapper).toAnonymousSearchSummaryDto(aedSummary)
    }
}
