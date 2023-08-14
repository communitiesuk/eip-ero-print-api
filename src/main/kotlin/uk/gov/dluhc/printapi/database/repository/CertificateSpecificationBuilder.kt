package uk.gov.dluhc.printapi.database.repository

import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.dto.VacSearchBy
import uk.gov.dluhc.printapi.dto.VacSearchCriteriaDto
import uk.gov.dluhc.printapi.service.sanitizeApplicationReference
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Root

@Component
class CertificateSpecificationBuilder {

    companion object {
        private const val GSS_CODE: String = "gssCode"
        private const val APPLICATION_REFERENCE: String = "applicationReference"
    }

    fun buildSpecification(
        gssCodes: List<String>,
        criteria: VacSearchCriteriaDto
    ): Specification<Certificate> {
        return buildSpecificationForGssCodes(gssCodes)
            .and(buildSpecificationForSearchBy(criteria.searchBy, criteria.searchValue))
    }

    private fun buildSpecificationForGssCodes(gssCodes: List<String>) =
        hasGssCodeIn(gssCodes)

    private fun buildSpecificationForSearchBy(
        searchBy: VacSearchBy?,
        searchValue: String?
    ): Specification<Certificate>? {
        if (searchBy == null || searchValue == null) {
            return null
        }

        return when (searchBy) {
            VacSearchBy.APPLICATION_REFERENCE -> hasApplicationReference(sanitizeApplicationReference(searchValue))
        }
    }

    private fun hasGssCodeIn(gssCodes: List<String>): Specification<Certificate> {
        return Specification { root: Root<Certificate>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            val query: CriteriaQuery<Certificate> = criteriaBuilder.createQuery(Certificate::class.java)
            query.select(root).where(root.get<Any>(GSS_CODE).`in`(gssCodes)).restriction
        }
    }

    private fun hasApplicationReference(applicationReference: String): Specification<Certificate> {
        return Specification<Certificate> { root: Root<Certificate?>, _: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            criteriaBuilder.equal(root.get<Any>(APPLICATION_REFERENCE), applicationReference)
        }
    }
}
