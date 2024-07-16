package uk.gov.dluhc.printapi.database.repository

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.dto.CertificateSearchBy
import uk.gov.dluhc.printapi.dto.CertificateSearchCriteriaDto
import uk.gov.dluhc.printapi.service.sanitizeApplicationReference
import uk.gov.dluhc.printapi.service.sanitizeSurname

@Component
class CertificateSpecificationBuilder {

    companion object {
        private const val GSS_CODE: String = "gssCode"
        private const val APPLICATION_REFERENCE: String = "applicationReference"
        private const val SANITIZED_SURNAME: String = "sanitizedSurname"
    }

    fun buildSpecification(
        gssCodes: List<String>,
        criteria: CertificateSearchCriteriaDto
    ): Specification<Certificate> {
        return buildSpecificationForGssCodes(gssCodes)
            .and(buildSpecificationForSearchBy(criteria.searchBy, criteria.searchValue))
    }

    private fun buildSpecificationForGssCodes(gssCodes: List<String>) =
        hasGssCodeIn(gssCodes)

    private fun buildSpecificationForSearchBy(
        searchBy: CertificateSearchBy?,
        searchValue: String?
    ): Specification<Certificate>? {
        if (searchBy == null || searchValue == null) {
            return null
        }

        return when (searchBy) {
            CertificateSearchBy.SURNAME -> hasSanitizedSurname(sanitizeSurname(searchValue))
            CertificateSearchBy.APPLICATION_REFERENCE -> hasApplicationReference(sanitizeApplicationReference(searchValue))
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

    private fun hasSanitizedSurname(surname: String): Specification<Certificate> {
        return Specification<Certificate> { root: Root<Certificate?>, query: CriteriaQuery<*>?, criteriaBuilder: CriteriaBuilder ->
            val printRequest: Join<PrintRequest, Certificate> = root.join("printRequests")
            query?.distinct(true)
            criteriaBuilder.equal(printRequest.get<Any>(SANITIZED_SURNAME), surname)
        }
    }
}
