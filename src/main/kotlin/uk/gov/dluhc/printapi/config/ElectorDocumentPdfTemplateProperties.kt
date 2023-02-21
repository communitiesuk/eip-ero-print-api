package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.AnonymousElectorDocumentPlaceholder
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.English
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.Welsh

@ConfigurationProperties(prefix = "temporary-certificate.certificate-pdf")
@ConstructorBinding
class TemporaryCertificatePdfTemplateProperties(
    english: English<English.TemporaryCertificatePlaceholder>,
    welsh: Welsh<Welsh.TemporaryCertificatePlaceholder>
) : ElectorDocumentPdfTemplateProperties<English.TemporaryCertificatePlaceholder, Welsh.TemporaryCertificatePlaceholder>(
    english,
    welsh
)

@ConfigurationProperties(prefix = "anonymous-elector-document.certificate-pdf")
@ConstructorBinding
class AnonymousElectorDocumentPdfTemplateProperties(
    english: English<AnonymousElectorDocumentPlaceholder>,
    welsh: Welsh<AnonymousElectorDocumentPlaceholder>
) : ElectorDocumentPdfTemplateProperties<AnonymousElectorDocumentPlaceholder, AnonymousElectorDocumentPlaceholder>(
    english,
    welsh
)

abstract class ElectorDocumentPdfTemplateProperties<EP, WP>(
    val english: English<EP>,
    val welsh: Welsh<WP>
) {
    data class English<T>(
        val path: String,
        val placeholder: T,
        val images: Images
    ) {
        data class TemporaryCertificatePlaceholder(
            val electorName: String,
            val localAuthorityNameEn: String,
            val dateOfIssue: String,
            val validOnDate: String,
            val certificateNumber: String
        )

        data class Images(
            val voterPhoto: PhotoProperties
        )
    }

    data class Welsh<T>(
        val path: String,
        val placeholder: T,
        val images: Images
    ) {
        data class TemporaryCertificatePlaceholder(
            val electorName: String,
            val localAuthorityNameEn: String,
            val localAuthorityNameCy: String,
            val dateOfIssue: String,
            val validOnDate: String,
            val certificateNumber: String
        )

        data class Images(
            val voterPhoto: PhotoProperties
        )
    }

    data class AnonymousElectorDocumentPlaceholder(
        val electoralRollNumber: String,
        val dateOfIssue: String,
        val certificateNumber: String
    )

    data class PhotoProperties(
        val pageNumber: Int = 1,
        val absoluteXMm: Float,
        val absoluteYMm: Float,
        val fitWidthMm: Float,
        val fitHeightMm: Float
    )
}
