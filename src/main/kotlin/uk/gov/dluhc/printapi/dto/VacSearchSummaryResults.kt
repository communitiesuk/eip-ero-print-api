package uk.gov.dluhc.printapi.dto

data class VacSearchSummaryResults(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<VacSummaryDto>,
)
