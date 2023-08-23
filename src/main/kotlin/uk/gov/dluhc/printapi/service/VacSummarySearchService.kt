package uk.gov.dluhc.printapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.mapper.VacSummaryDtoMapper
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateSpecificationBuilder
import uk.gov.dluhc.printapi.dto.VacSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.VacSearchSummaryResults

@Service
class VacSummarySearchService(
    private val eroService: EroService,
    private val certificateRepository: CertificateRepository,
    private val specificationBuilder: CertificateSpecificationBuilder,
    private val mapper: VacSummaryDtoMapper,
) {

    fun searchVacSummaries(
        criteria: VacSearchCriteriaDto
    ): VacSearchSummaryResults {
        val gssCodes = eroService.lookupGssCodesForEro(criteria.eroId)

        val pageRequest = buildPageRequest(
            page = criteria.page,
            pageSize = criteria.pageSize
        )
        val specification = specificationBuilder.buildSpecification(gssCodes, criteria)

        val vacSummaries = certificateRepository.findAll(specification, pageRequest)
            .map { mapper.certificateToVacSummaryDto(it) }
        return with(vacSummaries) {
            VacSearchSummaryResults(
                page = if (totalPages > 0) criteria.page else totalPages,
                pageSize = criteria.pageSize,
                totalPages = totalPages,
                totalResults = totalElements.toInt(),
                results = content
            )
        }
    }

    private fun buildPageRequest(page: Int, pageSize: Int): Pageable {
        val sortByIssueDateDesc = Sort.by(Sort.Direction.DESC, "issueDate")
        val sortByReference = Sort.by(Sort.Direction.ASC, "applicationReference")
        val repositoryPageIndex = page - 1
        return PageRequest.of(repositoryPageIndex, pageSize, sortByIssueDateDesc.and(sortByReference))
    }
}
