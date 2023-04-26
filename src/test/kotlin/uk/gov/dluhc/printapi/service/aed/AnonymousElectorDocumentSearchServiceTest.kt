package uk.gov.dluhc.printapi.service.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.rest.aed.AedSearchQueryStringParameters
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocumentSummaryEntity
import uk.gov.dluhc.printapi.testsupport.testdata.entity.withPageRequestAndSortOrder

@ExtendWith(MockitoExtension::class)
internal class AnonymousElectorDocumentSearchServiceTest {

    @Mock
    private lateinit var eroService: EroService

    @Mock
    private lateinit var anonymousElectorDocumentSummaryRepository: AnonymousElectorDocumentSummaryRepository

    @Mock
    private lateinit var anonymousSearchSummaryMapper: AnonymousSearchSummaryMapper

    @InjectMocks
    private lateinit var anonymousElectorDocumentSearchService: AnonymousElectorDocumentSearchService

    @Test
    fun `should return empty Anonymous Elector Document summaries`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        val searchCriteria = AedSearchQueryStringParameters()

        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        given(anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceType(any(), any(), any())).willReturn(Page.empty())

        // When
        val actualPagedRecords =
            anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(eroId, searchCriteria)

        // Then
        assertThat(actualPagedRecords).isNotNull.isEmpty()
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(anonymousElectorDocumentSummaryRepository).findAllByGssCodeInAndSourceType(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, withPageRequestAndSortOrder())
        verifyNoInteractions(anonymousSearchSummaryMapper)
    }

    @Test
    fun `should return Anonymous Elector Document summaries`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        val aedSummary = buildAnonymousElectorDocumentSummaryEntity()
        val expectedSummaryDto = buildAnonymousSearchSummaryDto()
        val pageRequest = withPageRequestAndSortOrder(page = 2)
        val searchCriteria = AedSearchQueryStringParameters(page = 2)

        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        given(anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceType(any(), any(), any()))
            .willReturn(PageImpl(listOf(aedSummary), pageRequest, 1))
        given(anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(any())).willReturn(expectedSummaryDto)

        val expected = PageImpl(mutableListOf(expectedSummaryDto), pageRequest, 1)

        // When
        val actualPagedRecords =
            anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(eroId, searchCriteria)

        // Then
        assertThat(actualPagedRecords).usingRecursiveComparison().isEqualTo(expected)
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(anonymousElectorDocumentSummaryRepository).findAllByGssCodeInAndSourceType(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, pageRequest)
        verify(anonymousSearchSummaryMapper).toAnonymousSearchSummaryDto(aedSummary)
    }
}
