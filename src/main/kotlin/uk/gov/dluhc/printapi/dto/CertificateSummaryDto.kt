package uk.gov.dluhc.printapi.dto

data class CertificateSummaryDto(
    val vacNumber: String,
    val sourceReference: String,
    val applicationReference: String,
    val photoLocationArn: String,
    val firstName: String,
    val middleNames: String?,
    val surname: String,
    val printRequests: List<PrintRequestSummaryDto>
)
