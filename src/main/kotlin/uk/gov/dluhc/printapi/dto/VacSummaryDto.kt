package uk.gov.dluhc.printapi.dto

data class VacSummaryDto(
    val vacNumber: String,
    val sourceReference: String,
    val applicationReference: String,
    val firstName: String,
    val middleNames: String?,
    val surname: String,
    val printRequests: List<VacPrintRequestSummaryDto>
)
