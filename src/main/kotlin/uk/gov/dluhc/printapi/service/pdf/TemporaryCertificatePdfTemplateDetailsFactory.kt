package uk.gov.dluhc.printapi.service.pdf

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.config.TemporaryCertificatePdfTemplateProperties
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.service.isWalesCode

@Component
class TemporaryCertificatePdfTemplateDetailsFactory(
    s3Client: S3Client,
    private val pdfTemplateProperties: TemporaryCertificatePdfTemplateProperties
) : AbstractElectorDocumentPdfTemplateDetailsFactory(s3Client, pdfTemplateProperties) {

    fun getTemplateDetails(certificate: TemporaryCertificate): TemplateDetails =
        buildDetails(certificate.gssCode!!, certificate.photoLocationArn!!, getPlaceholders(certificate))

    private fun getPlaceholders(certificate: TemporaryCertificate): Map<String, String> {
        return if (isWalesCode(certificate.gssCode!!)) {
            getWelshTemplatePlaceholders(certificate)
        } else {
            getEnglishTemplatePlaceholders(certificate)
        }
    }

    private fun getWelshTemplatePlaceholders(certificate: TemporaryCertificate): Map<String, String> {
        return mapOf(
            pdfTemplateProperties.welsh.placeholder.electorName to certificate.getNameOnCertificate(),
            pdfTemplateProperties.welsh.placeholder.localAuthorityNameCy to certificate.issuingAuthorityCy!!,
            pdfTemplateProperties.welsh.placeholder.localAuthorityNameEn to certificate.issuingAuthority!!,
            pdfTemplateProperties.welsh.placeholder.dateOfIssue to certificate.issueDate.format(DATE_TIME_FORMATTER),
            pdfTemplateProperties.welsh.placeholder.validOnDate to certificate.validOnDate!!.format(DATE_TIME_FORMATTER),
            pdfTemplateProperties.welsh.placeholder.certificateNumber to certificate.certificateNumber!!,
        )
    }

    private fun getEnglishTemplatePlaceholders(certificate: TemporaryCertificate): Map<String, String> {
        return mapOf(
            pdfTemplateProperties.english.placeholder.electorName to certificate.getNameOnCertificate(),
            pdfTemplateProperties.english.placeholder.localAuthorityNameEn to certificate.issuingAuthority!!,
            pdfTemplateProperties.english.placeholder.dateOfIssue to certificate.issueDate.format(DATE_TIME_FORMATTER),
            pdfTemplateProperties.english.placeholder.validOnDate to certificate.validOnDate!!.format(
                DATE_TIME_FORMATTER
            ),
            pdfTemplateProperties.english.placeholder.certificateNumber to certificate.certificateNumber!!,
        )
    }
}
