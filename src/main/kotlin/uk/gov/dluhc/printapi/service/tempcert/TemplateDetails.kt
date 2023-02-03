package uk.gov.dluhc.printapi.service.tempcert

data class TemplateDetails(
    val path: String,
    val placeholders: Map<String, String>
)
