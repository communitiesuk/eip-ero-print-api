package uk.gov.dluhc.printapi.service.aed

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.rest.aed.AedSearchQueryStringParameters
import uk.gov.dluhc.printapi.service.EroService

@Service
class AnonymousElectorDocumentSearchService(
    private val eroService: EroService,
    private val anonymousElectorDocumentSummaryRepository: AnonymousElectorDocumentSummaryRepository,
    private val anonymousSearchSummaryMapper: AnonymousSearchSummaryMapper,
) {

    fun searchAnonymousElectorDocumentSummaries(
        eroId: String,
        searchCriteria: AedSearchQueryStringParameters
    ): Page<AnonymousSearchSummaryDto> {
        val gssCodes = eroService.lookupGssCodesForEro(eroId)
        val sortByIssueDateDesc = Sort.by(DESC, "issueDate")
        val sortBySurnameAsc = Sort.by(ASC, "surname")
        val repositoryPageIndex = searchCriteria.page - 1
        val pageRequest = PageRequest.of(
            repositoryPageIndex,
            searchCriteria.pageSize,
            sortByIssueDateDesc.and(sortBySurnameAsc)
        )
        return anonymousElectorDocumentSummaryRepository
            .findAllByGssCodeInAndSourceType(
                gssCodes = gssCodes,
                sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
                pageRequest = pageRequest
            ).map { anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(it) }
    }
}
