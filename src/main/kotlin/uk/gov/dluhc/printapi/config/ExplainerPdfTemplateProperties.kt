package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "temporary-certificate.explainer-pdf")
class TemporaryCertificateExplainerPdfTemplateProperties(downloadFilenamePrefix: String, english: English, welsh: Welsh) :
    ExplainerPdfTemplateProperties(downloadFilenamePrefix, english, welsh)

@ConfigurationProperties(prefix = "anonymous-elector-document.explainer-pdf")
class AnonymousElectorDocumentExplainerPdfTemplateProperties(downloadFilenamePrefix: String, english: English, welsh: Welsh) :
    ExplainerPdfTemplateProperties(downloadFilenamePrefix, english, welsh)

abstract class ExplainerPdfTemplateProperties(
    val downloadFilenamePrefix: String,
    val english: English,
    val welsh: Welsh,
) {
    data class English(
        val path: String,
        val placeholder: Placeholder,
    )

    data class Welsh(
        val path: String,
        val placeholder: Placeholder,
    )

    data class Placeholder(
        val contactDetails1: String,
        val contactDetails2: String,
        val contactDetails3: String,
        val contactDetails4: String,
        val contactDetails5: String,
        val contactDetails6: String,
        val contactDetails7: String,
        val contactDetails8: String,
    )
}
