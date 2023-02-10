package uk.gov.dluhc.printapi.service.temporarycertificate

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import java.io.File
import java.io.FileOutputStream

internal class PdfFactoryTest {

    companion object {
        // Explainer templates
        private const val EXPLAINER_PDF_TEMPLATE_ENGLISH =
            "classpath:temporary-certificate-template/Explainer Document (English).pdf"
        private const val EXPLAINER_PDF_TEMPLATE_WELSH =
            "classpath:temporary-certificate-template/Explainer Document (Dual Language).pdf"

        // Certificate templates
        private const val CERTIFICATE_PDF_TEMPLATE_ENGLISH =
            "classpath:temporary-certificate-template/Temp Voter Authority Certificate (English).pdf"
        private const val CERTIFICATE_PDF_TEMPLATE_WELSH =
            "classpath:temporary-certificate-template/Temp Voter Authority Certificate (Bilingual).pdf"
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
                // Commented out as the provided template does not include these form fields
                // "ero-address-3-en" to address.town.orEmpty(),
                // "ero-address-4-en" to address.area.orEmpty(),
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
                // Commented out as the provided template does not include these form fields
                // "ero-address-3-cy" to address.town.orEmpty(),
                // "ero-address-4-cy" to address.area.orEmpty(),
                "ero-postcode-cy" to address.postcode,
                "ero-email-cy" to emailAddress,
                "ero-phonenumber-cy" to phoneNumber,
            )
        }
        val templateDetails = TemplateDetails(EXPLAINER_PDF_TEMPLATE_WELSH, placeholders)

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        FileOutputStream(File("Example_EXPLAINER_English.pdf")).use { it.write(contents) }
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    @Test
    fun `should create temporary certificate PDF with placeholders and image filled for English template`() {
        // Given
        val placeholders = mapOf(
            "applicant-name" to "John Smith",
            "date-issued" to "20/04/2023",
            "date-valid" to "04/05/2023",
            "certificate-number" to "TlbBclMIWfyQhaWxk0Zy",
            "local-authority-name-en" to "Vale of White Horse District Council",

        )
        val imageBytes = ResourceUtils.getFile(CERTIFICATE_SAMPLE_PHOTO).readBytes()
        val imageDetails =
            ImageDetails(absoluteX = 21.6f, absoluteY = 201.6f, fitWidth = 35f, fitHeight = 45f, bytes = imageBytes)
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
            "applicant-name" to "Joe Blogs",
            "date-issued" to "26/04/2023",
            "date-valid" to "06/05/2023",
            "certificate-number" to "G1eQIZSYOhP7AeKnhJ8E",
            "local-authority-name-en" to "Merthyr Tydfil County Borough Council",
            "local-authority-name-cy" to "Cyngor Bwrdeistref Sirol Merthyr Tudful",
        )
        val imageBytes = ResourceUtils.getFile(CERTIFICATE_SAMPLE_PHOTO).readBytes()
        val imageDetails =
            ImageDetails(absoluteX = 21.6f, absoluteY = 194.6f, fitWidth = 35f, fitHeight = 45f, bytes = imageBytes)
        val templateDetails = TemplateDetails(CERTIFICATE_PDF_TEMPLATE_WELSH, placeholders, listOf(imageDetails))

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        FileOutputStream(File("Example_CERTIFICATE_Welsh.pdf")).use { it.write(contents) }
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    private fun verifyGeneratedPdfPlaceholders(contents: ByteArray, placeholders: Map<String, String>) {
        PdfReader(contents).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            placeholders.values.forEach { assertThat(text).contains(it) }
        }
    }
}
