package uk.gov.dluhc.printapi.dto.aed

data class AnonymousSearchCriteriaDto(
    val eroId: String,
    val page: Int,
    val pageSize: Int,
    val searchBy: AedSearchBy? = null,
    val searchValue: String? = null,
)

enum class AedSearchBy {
    SURNAME,
    APPLICATION_REFERENCE,
}
