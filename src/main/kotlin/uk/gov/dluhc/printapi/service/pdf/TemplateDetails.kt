package uk.gov.dluhc.printapi.service.pdf

data class TemplateDetails(
    val path: String,
    val placeholders: Map<String, String>,
    val images: List<ImageDetails> = listOf()
)
