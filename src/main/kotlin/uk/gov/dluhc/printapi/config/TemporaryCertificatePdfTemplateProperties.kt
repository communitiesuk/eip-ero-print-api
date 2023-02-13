package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties(prefix = "temporary-certificate.certificate-pdf")
@ConstructorBinding
data class TemporaryCertificatePdfTemplateProperties(
    val english: English,
    val welsh: Welsh
) {
    data class English(
        val path: String,
        val placeholder: Placeholder,
        val images: Images
    ) {
        data class Placeholder(
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

    data class Welsh(
        val path: String,
        val placeholder: Placeholder,
        val images: Images
    ) {
        data class Placeholder(
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

    data class PhotoProperties(
        val pageNumber: Int = 1,
        val absoluteXMm: Float,
        val absoluteYMm: Float,
        val fitWidthMm: Float,
        val fitHeightMm: Float
    )
}
