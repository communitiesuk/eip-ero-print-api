package uk.gov.dluhc.printapi.service.aed

import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.config.AnonymousElectorDocumentPdfTemplateProperties
import uk.gov.dluhc.printapi.database.entity.AedPrintRequest
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
        val printRequest = electorDocument.getLatestPrintRequest()
        return if (isWalesCode(electorDocument.gssCode)) {
            getWelshTemplatePlaceholders(electorDocument, printRequest)
        } else {
            getEnglishTemplatePlaceholders(electorDocument, printRequest)
        }
    }

    private fun getWelshTemplatePlaceholders(
        electorDocument: AnonymousElectorDocument,
        printRequest: AedPrintRequest
    ): Map<String, String> {
        return mapOf(
            pdfTemplateProperties.welsh.placeholder.electoralRollNumber to printRequest.electoralRollNumber,
            pdfTemplateProperties.welsh.placeholder.dateOfIssue to printRequest.issueDate.format(DATE_TIME_FORMATTER),
            pdfTemplateProperties.welsh.placeholder.certificateNumber to electorDocument.certificateNumber,
        )
    }

    private fun getEnglishTemplatePlaceholders(
        electorDocument: AnonymousElectorDocument,
        printRequest: AedPrintRequest
    ): Map<String, String> {
        return mapOf(
            pdfTemplateProperties.english.placeholder.electoralRollNumber to printRequest.electoralRollNumber,
            pdfTemplateProperties.english.placeholder.dateOfIssue to printRequest.issueDate.format(DATE_TIME_FORMATTER),
            pdfTemplateProperties.english.placeholder.certificateNumber to electorDocument.certificateNumber,
        )
    }
}
