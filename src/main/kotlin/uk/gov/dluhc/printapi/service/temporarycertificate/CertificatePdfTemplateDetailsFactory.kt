package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.service.isWalesCode
import java.time.format.DateTimeFormatter

@Component
class CertificatePdfTemplateDetailsFactory(
    @Value("\${temporary-certificate.certificate-pdf.english.path}") private val pdfTemplateEnglish: String,
    @Value("\${temporary-certificate.certificate-pdf.english.placeholder.elector-name}") private val electorNameEnPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.english.placeholder.local-authority-name}") private val localAuthorityNameEnPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.english.placeholder.date-of-issue}") private val dateOfIssueEnPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.english.placeholder.valid-on-date}") private val validOnDateEnPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.english.placeholder.certificate-number}") private val certificateNumberEnPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.english.images.voter-photo.page-number}") private val voterPhotoPageNoEnPlaceholder: Int,
    @Value("\${temporary-certificate.certificate-pdf.english.images.voter-photo.absolute-x}") private val voterPhotoAbsoluteXEnPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.english.images.voter-photo.absolute-y}") private val voterPhotoAbsoluteYEnPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.english.images.voter-photo.fit-width}") private val voterPhotoFitWidthEnPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.english.images.voter-photo.fit-height}") private val voterPhotoFitHeightEnPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.welsh.path}") private val pdfTemplateWelsh: String,
    @Value("\${temporary-certificate.certificate-pdf.welsh.placeholder.elector-name}") private val electorNameCyPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.welsh.placeholder.local-authority-name}") private val localAuthorityNameCyPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.welsh.placeholder.date-of-issue}") private val dateOfIssueCyPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.welsh.placeholder.valid-on-date}") private val validOnDateCyPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.welsh.placeholder.certificate-number}") private val certificateNumberCyPlaceholder: String,
    @Value("\${temporary-certificate.certificate-pdf.welsh.images.voter-photo.page-number}") private val voterPhotoPageNoCyPlaceholder: Int,
    @Value("\${temporary-certificate.certificate-pdf.welsh.images.voter-photo.absolute-x}") private val voterPhotoAbsoluteXCyPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.welsh.images.voter-photo.absolute-y}") private val voterPhotoAbsoluteYCyPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.welsh.images.voter-photo.fit-width}") private val voterPhotoFitWidthCyPlaceholder: Float,
    @Value("\${temporary-certificate.certificate-pdf.welsh.images.voter-photo.fit-height}") private val voterPhotoFitHeightCyPlaceholder: Float,
) {
    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }

    fun getTemplateDetails(certificate: TemporaryCertificate): TemplateDetails {
        val voterPhotoBytes = getPhotoBytes(certificate.photoLocationArn!!)
        return if (isWalesCode(certificate.gssCode!!)) {
            val images = listOf(getVoterPhotoImageCy(voterPhotoBytes))
            TemplateDetails(pdfTemplateWelsh, getWelshTemplatePlaceholders(certificate), images)
        } else {
            val images = listOf(getVoterPhotoImageEn(voterPhotoBytes))
            TemplateDetails(pdfTemplateEnglish, getEnglishTemplatePlaceholders(certificate), images)
        }
    }

    private fun getPhotoBytes(photoLocationArn: String): ByteArray {
        // TODO get photo from S3
        return byteArrayOf()
    }

    private fun getVoterPhotoImageEn(contents: ByteArray): ImageDetails {
        return ImageDetails(
            pageNumber = voterPhotoPageNoEnPlaceholder,
            absoluteX = voterPhotoAbsoluteXEnPlaceholder,
            absoluteY = voterPhotoAbsoluteYEnPlaceholder,
            fitWidth = voterPhotoFitWidthEnPlaceholder,
            fitHeight = voterPhotoFitHeightEnPlaceholder,
            bytes = contents
        )
    }

    private fun getVoterPhotoImageCy(contents: ByteArray): ImageDetails {
        return ImageDetails(
            pageNumber = voterPhotoPageNoCyPlaceholder,
            absoluteX = voterPhotoAbsoluteXCyPlaceholder,
            absoluteY = voterPhotoAbsoluteYCyPlaceholder,
            fitWidth = voterPhotoFitWidthCyPlaceholder,
            fitHeight = voterPhotoFitHeightCyPlaceholder,
            bytes = contents
        )
    }

    fun getTemplateFilename(gssCode: String): String {
        return (if (isWalesCode(gssCode)) pdfTemplateWelsh else pdfTemplateEnglish).substringAfterLast("/")
    }

    private fun getWelshTemplatePlaceholders(certificate: TemporaryCertificate): Map<String, String> {
        return mapOf(
            electorNameCyPlaceholder to certificate.getNameOnCertificate(),
            localAuthorityNameCyPlaceholder to certificate.issuingAuthorityCy!!,
            dateOfIssueCyPlaceholder to certificate.issueDate.format(DATE_TIME_FORMATTER),
            validOnDateCyPlaceholder to certificate.validOnDate!!.format(DATE_TIME_FORMATTER),
            certificateNumberCyPlaceholder to certificate.certificateNumber!!,
        )
    }

    private fun getEnglishTemplatePlaceholders(certificate: TemporaryCertificate): Map<String, String> {
        return mapOf(
            electorNameEnPlaceholder to certificate.getNameOnCertificate(),
            localAuthorityNameEnPlaceholder to certificate.issuingAuthority!!,
            dateOfIssueEnPlaceholder to certificate.issueDate.format(DATE_TIME_FORMATTER),
            validOnDateEnPlaceholder to certificate.validOnDate!!.format(DATE_TIME_FORMATTER),
            certificateNumberEnPlaceholder to certificate.certificateNumber!!,
        )
    }
}