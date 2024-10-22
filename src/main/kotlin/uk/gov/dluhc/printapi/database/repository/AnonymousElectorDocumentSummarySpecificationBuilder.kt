package uk.gov.dluhc.printapi.database.repository

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.dto.aed.AedSearchBy
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchCriteriaDto
import uk.gov.dluhc.printapi.service.sanitizeApplicationReference
import uk.gov.dluhc.printapi.service.sanitizeSurname

@Component
class AnonymousElectorDocumentSummarySpecificationBuilder {

    companion object {
        private const val GSS_CODE: String = "gssCode"
        private const val APPLICATION_REFERENCE: String = "applicationReference"
        private const val SANITIZED_SURNAME: String = "sanitizedSurname"
    }

    fun buildSpecification(
        gssCodes: List<String>,
        criteria: AnonymousSearchCriteriaDto
    ): Specification<AnonymousElectorDocumentSummary> {
        return buildSpecificationForGssCodes(gssCodes)
            .and(buildSpecificationForSearchBy(criteria.searchBy, criteria.searchValue))
    }

    private fun buildSpecificationForGssCodes(gssCodes: List<String>) =
        hasGssCodeIn(gssCodes)

    private fun buildSpecificationForSearchBy(
        searchBy: AedSearchBy?,
        searchValue: String?
    ): Specification<AnonymousElectorDocumentSummary>? {
        if (searchBy == null || searchValue == null) {
            return null
        }

        return when (searchBy) {
            AedSearchBy.SURNAME -> hasSanitizedSurname(sanitizeSurname(searchValue))
            AedSearchBy.APPLICATION_REFERENCE -> hasApplicationReference(sanitizeApplicationReference(searchValue))
        }
    }

    private fun hasGssCodeIn(gssCodes: List<String>): Specification<AnonymousElectorDocumentSummary> {
        return Specification { root: Root<AnonymousElectorDocumentSummary>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            val query: CriteriaQuery<AnonymousElectorDocumentSummary> = criteriaBuilder.createQuery(
                AnonymousElectorDocumentSummary::class.java
            )
            query.select(root).where(root.get<Any>(GSS_CODE).`in`(gssCodes)).restriction
        }
    }

    private fun hasApplicationReference(applicationReference: String): Specification<AnonymousElectorDocumentSummary> {
        return Specification<AnonymousElectorDocumentSummary> { root: Root<AnonymousElectorDocumentSummary?>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            criteriaBuilder.equal(root.get<Any>(APPLICATION_REFERENCE), applicationReference)
        }
    }

    private fun hasSanitizedSurname(sanitizedSurname: String): Specification<AnonymousElectorDocumentSummary> {
        return Specification<AnonymousElectorDocumentSummary> { root: Root<AnonymousElectorDocumentSummary?>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            criteriaBuilder.equal(root.get<Any>(SANITIZED_SURNAME), sanitizedSurname)
        }
    }
}
