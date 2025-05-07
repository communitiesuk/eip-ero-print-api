package uk.gov.dluhc.printapi.dto

data class CertificateSearchCriteriaDto(
    val eroId: String,
    val page: Int,
    val pageSize: Int,
    val searchBy: CertificateSearchBy? = null,
    val searchValue: String? = null,
)

enum class CertificateSearchBy {
    APPLICATION_REFERENCE,
    SURNAME,
}
