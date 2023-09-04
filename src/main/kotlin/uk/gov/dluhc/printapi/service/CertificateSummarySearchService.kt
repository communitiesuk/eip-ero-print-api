package uk.gov.dluhc.printapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.mapper.CertificateSummaryDtoMapper
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.CertificateSpecificationBuilder
import uk.gov.dluhc.printapi.dto.CertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.dto.CertificateSearchSummaryResults

@Service
class CertificateSummarySearchService(
    private val eroService: EroService,
    private val certificateRepository: CertificateRepository,
    private val specificationBuilder: CertificateSpecificationBuilder,
    private val mapper: CertificateSummaryDtoMapper,
) {

    fun searchCertificateSummaries(
        criteria: CertificateSearchCriteriaDto
    ): CertificateSearchSummaryResults {
        val gssCodes = eroService.lookupGssCodesForEro(criteria.eroId)

        val pageRequest = buildPageRequest(
            page = criteria.page,
            pageSize = criteria.pageSize
        )
        val specification = specificationBuilder.buildSpecification(gssCodes, criteria)

        val certificateSummaries = certificateRepository.findAll(specification, pageRequest)
            .map { mapper.certificateToCertificatePrintRequestSummaryDto(it) }
        return with(certificateSummaries) {
            CertificateSearchSummaryResults(
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
