package uk.gov.dluhc.printapi.service.temporarycertificate

import com.lowagie.text.pdf.AcroFields
import com.lowagie.text.pdf.PdfReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto

internal class PdfFactoryTest {

    companion object {
        // Explainer templates
        private const val EXPLAINER_PDF_TEMPLATE_ENGLISH =
            "classpath:temporary-certificate-template/Explainer Document (English).pdf"
        private const val EXPLAINER_PDF_TEMPLATE_WELSH =
            "classpath:temporary-certificate-template/Explainer Document (Dual Language).pdf"

        // Certificate templates
        private const val CERTIFICATE_PDF_TEMPLATE_ENGLISH =
            "classpath:temporary-certificate-template/Temporary Certificate (English).pdf"
        private const val CERTIFICATE_PDF_TEMPLATE_WELSH =
            "classpath:temporary-certificate-template/Temporary Certificate (Dual Language).pdf"
        private const val CERTIFICATE_SAMPLE_PHOTO =
            "classpath:temporary-certificate-template/sample-certificate-photo.png"
    }

    private var pdfFactory: PdfFactory = PdfFactory()

    @Test
    fun `should create explainer PDF with placeholders filled for English template`() {
        // Given
        val eroDetails = buildEroDto()
        val placeholders = with(eroDetails.englishContactDetails) {
            mapOf(
                "ero-recipient" to name,
                "ero-address-1-en" to address.property.orEmpty(),
                "ero-address-2-en" to address.street,
                "ero-address-3-en" to address.town.orEmpty(),
                "ero-address-4-en" to address.area.orEmpty(),
                "ero-postcode-en" to address.postcode,
                "ero-email-en" to emailAddress,
                "ero-phonenumber-en" to phoneNumber,
            )
        }
        val templateDetails = TemplateDetails(EXPLAINER_PDF_TEMPLATE_ENGLISH, placeholders)

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    @Test
    fun `should create explainer PDF with placeholders filled for Welsh template`() {
        // Given
        val eroDetails = buildEroDto()
        val placeholders = with(eroDetails.welshContactDetails!!) {
            mapOf(
                "ero-recipient" to name,
                "ero-address-1-cy" to address.property.orEmpty(),
                "ero-address-2-cy" to address.street,
                "ero-address-3-cy" to address.town.orEmpty(),
                "ero-address-4-cy" to address.area.orEmpty(),
                "ero-postcode-cy" to address.postcode,
                "ero-email-cy" to emailAddress,
                "ero-phonenumber-cy" to phoneNumber,
            )
        }
        val templateDetails = TemplateDetails(EXPLAINER_PDF_TEMPLATE_WELSH, placeholders)

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    @Test
    fun `should create temporary certificate PDF with placeholders and image filled for English template`() {
        // Given
        val placeholders = mapOf(
            "name" to "John Smith",
            "nameAdditonalLine" to "",
            "dateOfIssue" to "20/04/2023",
            "validOn" to "04/05/2023",
            "certificateNumber" to "TlbBclMIWfyQhaWxk0Zy",
            "localAuthorityEn" to "Vale of White Horse District Council",
            "localAuthorityAdditionalLine" to "",

        )
        val imageBytes = ResourceUtils.getFile(CERTIFICATE_SAMPLE_PHOTO).readBytes()
        val imageDetails =
            ImageDetails(absoluteX = 62f, absoluteY = 568f, fitWidth = 99f, fitHeight = 127f, bytes = imageBytes)
        val templateDetails = TemplateDetails(CERTIFICATE_PDF_TEMPLATE_ENGLISH, placeholders, listOf(imageDetails))

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    @Test
    fun `should create temporary certificate PDF with placeholders and image filled for Welsh template`() {
        // Given
        val placeholders = mapOf(
            "name" to "Joe Blogs",
            "nameAdditionalLine" to "",
            "dateOfIssue" to "26/04/2023",
            "validOn" to "06/05/2023",
            "certificateNumber" to "G1eQIZSYOhP7AeKnhJ8E",
            "localAuthorityEn" to "Merthyr Tydfil County Borough Council",
            "localAuthorityAdditionalLine" to "",
            "localAuthorityCy" to "Cyngor Bwrdeistref Sirol Merthyr Tudful",
        )
        val imageBytes = ResourceUtils.getFile(CERTIFICATE_SAMPLE_PHOTO).readBytes()
        val imageDetails =
            ImageDetails(absoluteX = 62f, absoluteY = 547f, fitWidth = 99f, fitHeight = 127f, bytes = imageBytes)
        val templateDetails = TemplateDetails(CERTIFICATE_PDF_TEMPLATE_WELSH, placeholders, listOf(imageDetails))

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    private fun verifyGeneratedPdfPlaceholders(contents: ByteArray, placeholders: Map<String, String>) {
        PdfReader(contents).use { reader -> verifyPlaceholders(reader.acroFields, placeholders) }
    }

    private fun verifyPlaceholders(form: AcroFields, placeholders: Map<String, String>) {
        for (field in form.allFields) {
            assertThat(placeholders).containsKey(field.key)
            assertThat(form.getField(field.key)).isEqualTo(placeholders[field.key])
        }
    }
}
