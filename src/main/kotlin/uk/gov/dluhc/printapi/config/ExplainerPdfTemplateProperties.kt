package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "temporary-certificate.explainer-pdf")
@ConstructorBinding
class TemporaryCertificateExplainerPdfTemplateProperties(filenamePrefix: String, english: English, welsh: Welsh) :
    ExplainerPdfTemplateProperties(filenamePrefix, english, welsh)

@ConfigurationProperties(prefix = "anonymous-elector-document.explainer-pdf")
@ConstructorBinding
class AnonymousElectorDocumentExplainerPdfTemplateProperties(filenamePrefix: String, english: English, welsh: Welsh) :
    ExplainerPdfTemplateProperties(filenamePrefix, english, welsh)

abstract class ExplainerPdfTemplateProperties(
    val filenamePrefix: String,
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
