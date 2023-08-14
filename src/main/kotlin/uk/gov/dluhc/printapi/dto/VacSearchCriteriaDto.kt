package uk.gov.dluhc.printapi.dto

data class VacSearchCriteriaDto(
    val eroId: String,
    val page: Int,
    val pageSize: Int,
    val searchBy: VacSearchBy? = null,
    val searchValue: String? = null,
)

enum class VacSearchBy {
    APPLICATION_REFERENCE,
}
