package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "temporary-certificate.explainer-pdf")
@ConstructorBinding
class TemporaryCertificateExplainerPdfTemplateProperties(
    val english: English,
    val welsh: Welsh
) {
    //  explainer-pdf:
    //     english:
    //       path: "classpath:temporary-certificate-template/Explainer Document (English).pdf"
    //       placeholder:
    //         ero-name: "ero-recipient"
    //         ero-address-line1: "ero-address-1-en"
    //         ero-address-line2: "ero-address-2-en"
    //         ero-address-line3: "ero-address-3-en"
    //         ero-address-line4: "ero-address-4-en"
    //         ero-address-postcode: "ero-postcode-en"
    //         ero-email: "ero-email-en"
    //         ero-phone: "ero-phonenumber-en"
    //     welsh:
    //       path: "classpath:temporary-certificate-template/Explainer Document (Dual Language).pdf"
    //       placeholder:
    //         ero-name: "ero-recipient"
    //         ero-address-line1: "ero-address-1-cy"
    //         ero-address-line2: "ero-address-2-cy"
    //         ero-address-line3: "ero-address-3-cy"
    //         ero-address-line4: "ero-address-4-cy"
    //         ero-address-postcode: "ero-postcode-cy"
    //         ero-email: "ero-email-cy"
    //         ero-phone: "ero-phonenumber-cy"
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
