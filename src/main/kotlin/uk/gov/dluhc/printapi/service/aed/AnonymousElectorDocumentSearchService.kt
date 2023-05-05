package uk.gov.dluhc.printapi.service.aed

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.dto.aed.AedSearchBy.APPLICATION_REFERENCE
import uk.gov.dluhc.printapi.dto.aed.AedSearchBy.SURNAME
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.service.EroService

@Service
class AnonymousElectorDocumentSearchService(
    private val eroService: EroService,
    private val anonymousElectorDocumentSummaryRepository: AnonymousElectorDocumentSummaryRepository,
    private val anonymousSearchSummaryMapper: AnonymousSearchSummaryMapper,
) {

    fun searchAnonymousElectorDocumentSummaries(
        dto: AnonymousSearchCriteriaDto
    ): AnonymousSearchSummaryResults {
        val gssCodes = eroService.lookupGssCodesForEro(dto.eroId)
        with(getAedPagedSummaries(dto = dto, gssCodes = gssCodes)) {
            return AnonymousSearchSummaryResults(
                page = dto.page,
                pageSize = dto.pageSize,
                totalPages = totalPages,
                totalResults = totalElements.toInt(),
                results = content
            )
        }
    }

    private fun getAedPagedSummaries(
        dto: AnonymousSearchCriteriaDto,
        gssCodes: List<String>
    ): Page<AnonymousSearchSummaryDto> {
        with(dto) {
            val pageRequest = buildPageRequest(page = page, pageSize = pageSize)
            return when (searchBy) {
                null ->
                    anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceType(
                        gssCodes = gssCodes,
                        sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                        pageRequest = pageRequest
                    )

                SURNAME ->
                    anonymousElectorDocumentSummaryRepository.findAllByGssCodeInAndSourceTypeAndSanitizedSurname(
                        gssCodes = gssCodes,
                        sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                        sanitizedSurname = sanitizeSurname(searchValue!!),
                        pageRequest = pageRequest
                    )

                APPLICATION_REFERENCE ->
                    throw UnsupportedOperationException("Searching AEDs by APPLICATION_REFERENCE is not yet supported")
            }.map { anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(entity = it) }
        }
    }

    private fun buildPageRequest(page: Int, pageSize: Int): Pageable {
        val sortByIssueDateDesc = Sort.by(DESC, "issueDate")
        val sortBySurnameAsc = Sort.by(ASC, "surname")
        val repositoryPageIndex = page - 1
        return PageRequest.of(repositoryPageIndex, pageSize, sortByIssueDateDesc.and(sortBySurnameAsc))
    }
}
