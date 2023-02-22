package uk.gov.dluhc.printapi.service.pdf

import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties
import uk.gov.dluhc.printapi.config.ElectorDocumentPdfTemplateProperties.PhotoProperties
import uk.gov.dluhc.printapi.service.isWalesCode
import uk.gov.dluhc.printapi.service.parseS3Arn
import java.time.format.DateTimeFormatter

abstract class AbstractElectorDocumentPdfTemplateDetailsFactory(
    private val s3Client: S3Client,
    private val pdfTemplateProperties: ElectorDocumentPdfTemplateProperties<*, *>
) {
    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    fun buildDetails(gssCode: String, photoArn: String, placeholders: Map<String, String>): TemplateDetails {
        val voterPhotoBytes = getPhotoBytes(photoArn)
        return if (isWalesCode(gssCode)) {
            val images = listOf(getVoterPhotoImage(pdfTemplateProperties.welsh.images.voterPhoto, voterPhotoBytes))
            TemplateDetails(pdfTemplateProperties.welsh.path, placeholders, images)
        } else {
            val images = listOf(getVoterPhotoImage(pdfTemplateProperties.english.images.voterPhoto, voterPhotoBytes))
            TemplateDetails(pdfTemplateProperties.english.path, placeholders, images)
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
        return (if (isWalesCode(gssCode)) pdfTemplateProperties.welsh.path else pdfTemplateProperties.english.path)
            .substringAfterLast("/")
    }
}
