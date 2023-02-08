package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.service.isWalesCode

@Component
class ExplainerPdfTemplateDetailsFactory(
    @Value("\${temporary-certificate.explainer-pdf.english.path}") private val pdfTemplateEnglish: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-name}") private val eroNameEnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-address-line1}") private val eroAddressLine1EnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-address-line2}") private val eroAddressLine2EnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-address-line3}") private val eroAddressLine3EnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-address-line4}") private val eroAddressLine4EnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-address-postcode}") private val eroAddressPostcodeEnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-email}") private val eroEmailAddressEnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.english.placeholder.ero-phone}") private val eroPhoneNumberEnPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.path}") private val pdfTemplateWelsh: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-name}") private val eroNameCyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-address-line1}") private val eroAddressLine1CyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-address-line2}") private val eroAddressLine2CyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-address-line3}") private val eroAddressLine3CyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-address-line4}") private val eroAddressLine4CyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-address-postcode}") private val eroAddressPostcodeCyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-email}") private val eroEmailAddressCyPlaceholder: String,
    @Value("\${temporary-certificate.explainer-pdf.welsh.placeholder.ero-phone}") private val eroPhoneNumberCyPlaceholder: String,
) {

    fun getTemplateDetails(gssCode: String, eroDto: EroDto): TemplateDetails {
        return if (isWalesCode(gssCode)) {
            TemplateDetails(pdfTemplateWelsh, getWelshTemplatePlaceholders(eroDto))
        } else {
            TemplateDetails(pdfTemplateEnglish, getEnglishTemplatePlaceholders(eroDto))
        }
    }

    private fun getWelshTemplatePlaceholders(eroDto: EroDto): Map<String, String> {
        val eroContactDetails = eroDto.welshContactDetails!!
        return mapOf(
            eroNameCyPlaceholder to eroContactDetails.name,
            eroAddressLine1CyPlaceholder to eroContactDetails.address.property.orEmpty(),
            eroAddressLine2CyPlaceholder to eroContactDetails.address.street,
            eroAddressLine3CyPlaceholder to eroContactDetails.address.town.orEmpty(),
            eroAddressLine4CyPlaceholder to eroContactDetails.address.area.orEmpty(),
            eroAddressPostcodeCyPlaceholder to eroContactDetails.address.postcode,
            eroEmailAddressCyPlaceholder to eroContactDetails.emailAddress,
            eroPhoneNumberCyPlaceholder to eroContactDetails.phoneNumber,
        )
    }

    private fun getEnglishTemplatePlaceholders(eroDto: EroDto): Map<String, String> {
        val eroContactDetails = eroDto.englishContactDetails
        return mapOf(
            eroNameEnPlaceholder to eroContactDetails.name,
            eroAddressLine1EnPlaceholder to eroContactDetails.address.property.orEmpty(),
            eroAddressLine2EnPlaceholder to eroContactDetails.address.street,
            eroAddressLine3EnPlaceholder to eroContactDetails.address.town.orEmpty(),
            eroAddressLine4EnPlaceholder to eroContactDetails.address.area.orEmpty(),
            eroAddressPostcodeEnPlaceholder to eroContactDetails.address.postcode,
            eroEmailAddressEnPlaceholder to eroContactDetails.emailAddress,
            eroPhoneNumberEnPlaceholder to eroContactDetails.phoneNumber,
        )
    }
}
