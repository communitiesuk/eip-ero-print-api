package uk.gov.dluhc.printapi.service.pdf

import uk.gov.dluhc.printapi.config.ExplainerPdfTemplateProperties
import uk.gov.dluhc.printapi.config.ExplainerPdfTemplateProperties.Placeholder
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.service.isWalesCode

class ExplainerPdfTemplateDetailsFactory(
    private val pdfTemplateProperties: ExplainerPdfTemplateProperties,
    private val exceptionMessageFunction: (eroId: String, gssCode: String) -> String
) {

    fun getDownloadFilenamePrefix(): String =
        pdfTemplateProperties.downloadFilenamePrefix

    fun getExceptionMessage(eroId: String, gssCode: String): String =
        exceptionMessageFunction(eroId, gssCode)

    fun getTemplateDetails(gssCode: String, eroDto: EroDto): TemplateDetails {
        return if (isWalesCode(gssCode)) {
            TemplateDetails(
                pdfTemplateProperties.welsh.path,
                getTemplatePlaceholders(eroDto.welshContactDetails!!, pdfTemplateProperties.welsh.placeholder)
            )
        } else {
            TemplateDetails(
                pdfTemplateProperties.english.path,
                getTemplatePlaceholders(eroDto.englishContactDetails, pdfTemplateProperties.english.placeholder)
            )
        }
    }

    private fun getTemplatePlaceholders(
        eroContactDetails: EroContactDetailsDto,
        pdfPlaceholders: Placeholder,
    ): Map<String, String> {
        val orderedNonBlankValues = listOfNotNull(
            eroContactDetails.name,
            eroContactDetails.address.property,
            eroContactDetails.address.street,
            eroContactDetails.address.town,
            eroContactDetails.address.area,
            eroContactDetails.address.postcode,
            eroContactDetails.emailAddress,
            eroContactDetails.phoneNumber
        ).filter { it.isNotBlank() }

        return mapOf(
            pdfPlaceholders.contactDetails1 to getValueOrBlank(orderedNonBlankValues, 0),
            pdfPlaceholders.contactDetails2 to getValueOrBlank(orderedNonBlankValues, 1),
            pdfPlaceholders.contactDetails3 to getValueOrBlank(orderedNonBlankValues, 2),
            pdfPlaceholders.contactDetails4 to getValueOrBlank(orderedNonBlankValues, 3),
            pdfPlaceholders.contactDetails5 to getValueOrBlank(orderedNonBlankValues, 4),
            pdfPlaceholders.contactDetails6 to getValueOrBlank(orderedNonBlankValues, 5),
            pdfPlaceholders.contactDetails7 to getValueOrBlank(orderedNonBlankValues, 6),
            pdfPlaceholders.contactDetails8 to getValueOrBlank(orderedNonBlankValues, 7),
        )
    }

    private fun getValueOrBlank(values: List<String>, index: Int): String =
        if (values.size > index) values[index] else ""
}
