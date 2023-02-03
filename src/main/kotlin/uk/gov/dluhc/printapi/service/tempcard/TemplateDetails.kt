package uk.gov.dluhc.printapi.service.tempcard

data class TemplateDetails(
    val path: String,
    val placeholders: Map<String, String>
)
