package uk.gov.dluhc.printapi.service.temporarycertificate

data class TemplateDetails(
    val path: String,
    val placeholders: Map<String, String>
)
