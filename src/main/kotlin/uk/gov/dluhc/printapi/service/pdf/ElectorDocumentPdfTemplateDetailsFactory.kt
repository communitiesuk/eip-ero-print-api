package uk.gov.dluhc.printapi.service.pdf

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.PhotoProperties
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.service.isWalesCode
import uk.gov.dluhc.printapi.service.parseS3Arn
import java.time.format.DateTimeFormatter

class ElectorDocumentPdfTemplateDetailsFactory(
    private val s3Client: S3Client,
    private val pdfTemplateProperties: ElectorDocumentPdfTemplateProperties
) {
    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    fun getTemplateDetails(certificate: TemporaryCertificate): TemplateDetails {
        val voterPhotoBytes = getPhotoBytes(certificate.photoLocationArn!!)
        return if (isWalesCode(certificate.gssCode!!)) {
            val images = listOf(getVoterPhotoImage(pdfTemplateProperties.welsh.images.voterPhoto, voterPhotoBytes))
            TemplateDetails(pdfTemplateProperties.welsh.path, getWelshTemplatePlaceholders(certificate), images)
        } else {
            val images = listOf(getVoterPhotoImage(pdfTemplateProperties.english.images.voterPhoto, voterPhotoBytes))
            TemplateDetails(pdfTemplateProperties.english.path, getEnglishTemplatePlaceholders(certificate), images)
        }
    }

    private fun getPhotoBytes(photoLocationArn: String): ByteArray {
        val s3Location = parseS3Arn(photoLocationArn)
        return s3Client.getObjectAsBytes(
            GetObjectRequest.builder().bucket(s3Location.bucket).key(s3Location.path).build()
        ).asByteArray()
    }

    private fun getVoterPhotoImage(photoProperties: PhotoProperties, contents: ByteArray): ImageDetails {
        return with(photoProperties) {
            ImageDetails(
                pageNumber = pageNumber,
                absoluteX = absoluteXMm,
                absoluteY = absoluteYMm,
                fitWidth = fitWidthMm,
                fitHeight = fitHeightMm,
                bytes = contents
            )
        }
    }

    fun getTemplateFilename(gssCode: String): String {
        return (if (isWalesCode(gssCode)) pdfTemplateProperties.welsh.path else pdfTemplateProperties.english.path).substringAfterLast("/")
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
