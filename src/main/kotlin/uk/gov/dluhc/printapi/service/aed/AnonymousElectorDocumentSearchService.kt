package uk.gov.dluhc.printapi.service.aed

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchCriteriaDto
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
        with(dto) {
            val gssCodes = eroService.lookupGssCodesForEro(eroId)
            val pagedSummaries = anonymousElectorDocumentSummaryRepository
                .findAllByGssCodeInAndSourceType(
                    gssCodes = gssCodes,
                    sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                    pageRequest = buildPageRequest(page = page, pageSize = pageSize)
                ).map { anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(it) }

            return AnonymousSearchSummaryResults(results = pagedSummaries.content)
        }
    }

    private fun buildPageRequest(page: Int, pageSize: Int): Pageable {
        val sortByIssueDateDesc = Sort.by(DESC, "issueDate")
        val sortBySurnameAsc = Sort.by(ASC, "surname")
        val repositoryPageIndex = page - 1
        return PageRequest.of(repositoryPageIndex, pageSize, sortByIssueDateDesc.and(sortBySurnameAsc))
    }
}
