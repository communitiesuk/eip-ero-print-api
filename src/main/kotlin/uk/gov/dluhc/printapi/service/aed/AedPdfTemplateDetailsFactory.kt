package uk.gov.dluhc.printapi.service.aed

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.config.AnonymousElectorDocumentPdfTemplateProperties
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.service.isWalesCode
import uk.gov.dluhc.printapi.service.pdf.AbstractElectorDocumentPdfTemplateDetailsFactory
import uk.gov.dluhc.printapi.service.pdf.TemplateDetails

@Component
class AedPdfTemplateDetailsFactory(
    s3Client: S3Client,
    private val pdfTemplateProperties: AnonymousElectorDocumentPdfTemplateProperties
) : AbstractElectorDocumentPdfTemplateDetailsFactory(s3Client, pdfTemplateProperties) {

    fun getTemplateDetails(electorDocument: AnonymousElectorDocument): TemplateDetails =
        buildDetails(electorDocument.gssCode, electorDocument.photoLocationArn, getPlaceholders(electorDocument))

    private fun getPlaceholders(electorDocument: AnonymousElectorDocument): Map<String, String> {
        return if (isWalesCode(electorDocument.gssCode)) {
            getWelshTemplatePlaceholders(electorDocument)
        } else {
            getEnglishTemplatePlaceholders(electorDocument)
        }
    }

    private fun getWelshTemplatePlaceholders(electorDocument: AnonymousElectorDocument): Map<String, String> {
        return with(electorDocument) {
            mapOf(
                pdfTemplateProperties.welsh.placeholder.electoralRollNumber to electoralRollNumber.uppercase(),
                pdfTemplateProperties.welsh.placeholder.dateOfIssue to issueDate.format(DATE_TIME_FORMATTER),
                pdfTemplateProperties.welsh.placeholder.certificateNumber to certificateNumber,
            )
        }
    }

    private fun getEnglishTemplatePlaceholders(
        electorDocument: AnonymousElectorDocument
    ): Map<String, String> {
        return with(electorDocument) {
            mapOf(
                pdfTemplateProperties.english.placeholder.electoralRollNumber to electoralRollNumber.uppercase(),
                pdfTemplateProperties.english.placeholder.dateOfIssue to issueDate.format(DATE_TIME_FORMATTER),
                pdfTemplateProperties.english.placeholder.certificateNumber to certificateNumber,
            )
        }
    }
}
