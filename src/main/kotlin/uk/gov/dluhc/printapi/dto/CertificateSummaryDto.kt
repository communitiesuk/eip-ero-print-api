package uk.gov.dluhc.printapi.dto

data class CertificateSummaryDto(
    val vacNumber: String,
    val printRequests: List<PrintRequestSummaryDto>
)
