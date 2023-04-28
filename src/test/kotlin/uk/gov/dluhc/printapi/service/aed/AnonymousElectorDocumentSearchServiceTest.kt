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
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.dto.aed.AedSearchBy
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aed.buildAnonymousSearchSummaryDto
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

    @InjectMocks
    private lateinit var anonymousElectorDocumentSearchService: AnonymousElectorDocumentSearchService

    @Test
    fun `should return empty Anonymous Elector Document summaries given null searchBy`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        val dto = buildAnonymousSearchCriteriaDto(eroId = eroId, searchBy = null)

        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        given(anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceType(any(), any(), any())).willReturn(Page.empty())

        // When
        val actualPagedRecords =
            anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(dto)

        // Then
        assertThat(actualPagedRecords).isNotNull
        assertThat(actualPagedRecords.results).isNotNull.isEmpty()
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(anonymousElectorDocumentSummaryRepository).findAllByGssCodeInAndSourceType(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, buildPageRequest())
        verifyNoInteractions(anonymousSearchSummaryMapper)
        verifyNoMoreInteractions(anonymousElectorDocumentSummaryRepository)
    }

    @Test
    fun `should return Anonymous Elector Document summaries given null searchBy`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        val aedSummary = buildAnonymousElectorDocumentSummaryEntity()
        val expectedSummaryDto = buildAnonymousSearchSummaryDto()
        val pageRequest = buildPageRequest(page = 2)
        val dto = buildAnonymousSearchCriteriaDto(eroId = eroId, page = 2, searchBy = null)

        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        given(anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceType(any(), any(), any()))
            .willReturn(PageImpl(listOf(aedSummary), pageRequest, 1))
        given(anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(any())).willReturn(expectedSummaryDto)

        val expected = AnonymousSearchSummaryResults(results = listOf(expectedSummaryDto))

        // When
        val actualPagedRecords =
            anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(dto)

        // Then
        assertThat(actualPagedRecords).usingRecursiveComparison().isEqualTo(expected)
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(anonymousElectorDocumentSummaryRepository).findAllByGssCodeInAndSourceType(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, pageRequest)
        verify(anonymousSearchSummaryMapper).toAnonymousSearchSummaryDto(aedSummary)
        verifyNoMoreInteractions(anonymousElectorDocumentSummaryRepository)
    }

    @Test
    fun `should return empty Anonymous Elector Document summaries given searchBy surname`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        val searchValue = "J-Smith O'Rorke   Junior"
        val sanitizedSurname = "J SMITH ORORKE JUNIOR"
        val dto = buildAnonymousSearchCriteriaDto(eroId = eroId, searchBy = AedSearchBy.SURNAME, searchValue = searchValue)

        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        given(anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceTypeAndSanitizedSurname(any(), any(), any(), any())).willReturn(Page.empty())

        // When
        val actualPagedRecords =
            anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(dto)

        // Then
        assertThat(actualPagedRecords).isNotNull
        assertThat(actualPagedRecords.results).isNotNull.isEmpty()
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(anonymousElectorDocumentSummaryRepository).findAllByGssCodeInAndSourceTypeAndSanitizedSurname(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, sanitizedSurname, buildPageRequest())
        verifyNoInteractions(anonymousSearchSummaryMapper)
        verifyNoMoreInteractions(anonymousElectorDocumentSummaryRepository)
    }

    @Test
    fun `should return Anonymous Elector Document summaries given searchBy surname`() {
        // Given
        val eroId = aValidRandomEroId()
        val gssCodes = listOf(aGssCode())
        val aedSummary = buildAnonymousElectorDocumentSummaryEntity()
        val expectedSummaryDto = buildAnonymousSearchSummaryDto()
        val pageRequest = buildPageRequest(page = 2)
        val searchValue = "J-Smith O'Rorke   Junior"
        val sanitizedSurname = "J SMITH ORORKE JUNIOR"
        val dto = buildAnonymousSearchCriteriaDto(
            eroId = eroId,
            page = 2,
            searchBy = AedSearchBy.SURNAME,
            searchValue = searchValue
        )

        given(eroService.lookupGssCodesForEro(any())).willReturn(gssCodes)
        given(anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceTypeAndSanitizedSurname(any(), any(), any(), any()))
            .willReturn(PageImpl(listOf(aedSummary), pageRequest, 1))
        given(anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(any())).willReturn(expectedSummaryDto)

        val expected = AnonymousSearchSummaryResults(results = listOf(expectedSummaryDto))

        // When
        val actualPagedRecords =
            anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(dto)

        // Then
        assertThat(actualPagedRecords).usingRecursiveComparison().isEqualTo(expected)
        verify(eroService).lookupGssCodesForEro(eroId)
        verify(anonymousElectorDocumentSummaryRepository).findAllByGssCodeInAndSourceTypeAndSanitizedSurname(gssCodes, ANONYMOUS_ELECTOR_DOCUMENT, sanitizedSurname, pageRequest)
        verify(anonymousSearchSummaryMapper).toAnonymousSearchSummaryDto(aedSummary)
        verifyNoMoreInteractions(anonymousElectorDocumentSummaryRepository)
    }
}
