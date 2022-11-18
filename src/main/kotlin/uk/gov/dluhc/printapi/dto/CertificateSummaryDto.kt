package uk.gov.dluhc.printapi.dto

data class CertificateSummaryDto(
    private val vacNumber: String,
    private val printRequests: List<PrintRequestSummaryDto>
)
