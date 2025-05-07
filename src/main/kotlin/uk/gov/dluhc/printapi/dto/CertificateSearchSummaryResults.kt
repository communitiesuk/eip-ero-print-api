package uk.gov.dluhc.printapi.dto

data class CertificateSearchSummaryResults(
    val page: Int,
    val pageSize: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<CertificateSummaryDto>,
)
