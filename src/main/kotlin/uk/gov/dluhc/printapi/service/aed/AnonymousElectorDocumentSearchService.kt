package uk.gov.dluhc.printapi.service.aed

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.domain.Sort.Direction.ASC
import org.springframework.data.domain.Sort.Direction.DESC
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummarySpecificationBuilder
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.service.EroService

@Service
class AnonymousElectorDocumentSearchService(
    private val eroService: EroService,
    private val anonymousElectorDocumentSummaryRepository: AnonymousElectorDocumentSummaryRepository,
    private val anonymousSearchSummaryMapper: AnonymousSearchSummaryMapper,
    private val specificationBuilder: AnonymousElectorDocumentSummarySpecificationBuilder,
) {

    fun searchAnonymousElectorDocumentSummaries(
        criteria: AnonymousSearchCriteriaDto
    ): AnonymousSearchSummaryResults {
        val gssCodes = eroService.lookupGssCodesForEro(criteria.eroId)

        val pageRequest = buildPageRequest(
            page = criteria.page,
            pageSize = criteria.pageSize
        )
        val specification = specificationBuilder.buildSpecification(gssCodes, criteria)

        val aedSummaries = anonymousElectorDocumentSummaryRepository.findAll(specification, pageRequest)
            .map { anonymousSearchSummaryMapper.toAnonymousSearchSummaryDto(entity = it) }
        return with(aedSummaries) {
            AnonymousSearchSummaryResults(
                page = if (totalPages > 0) criteria.page else totalPages,
                pageSize = criteria.pageSize,
                totalPages = totalPages,
                totalResults = totalElements.toInt(),
                results = content
            )
        }
    }

    private fun buildPageRequest(page: Int, pageSize: Int): Pageable {
        val sortByIssueDateDesc = Sort.by(DESC, "issueDate")
        val sortBySurnameAsc = Sort.by(ASC, "surname")
        val repositoryPageIndex = page - 1
        return PageRequest.of(repositoryPageIndex, pageSize, sortByIssueDateDesc.and(sortBySurnameAsc))
    }
}
