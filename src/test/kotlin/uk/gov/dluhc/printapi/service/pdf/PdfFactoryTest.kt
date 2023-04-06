package uk.gov.dluhc.printapi.service.pdf

import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.parser.PdfTextExtractor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto

internal class PdfFactoryTest {

    companion object {
        // Explainer templates
        private const val EXPLAINER_PDF_TEMPLATE_ENGLISH =
            "classpath:temporary-certificate-template/Temp Voter Authority Explainer (English) v1.pdf"
        private const val EXPLAINER_PDF_TEMPLATE_WELSH =
            "classpath:temporary-certificate-template/Temp Voter Authority Explainer (Bilingual) v1.pdf"

        // Certificate templates
        private const val CERTIFICATE_PDF_TEMPLATE_ENGLISH =
            "classpath:temporary-certificate-template/Temp Voter Authority Certificate (English) v1.pdf"
        private const val CERTIFICATE_PDF_TEMPLATE_WELSH =
            "classpath:temporary-certificate-template/Temp Voter Authority Certificate (Bilingual) v1.pdf"

        // AED templates
        private const val AED_PDF_TEMPLATE_ENGLISH =
            "classpath:anonymous-elector-document-template/AED Document (English) v2.pdf"
        private const val AED_PDF_TEMPLATE_WELSH =
            "classpath:anonymous-elector-document-template/AED Document (Bilingual) v2.pdf"
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
                "ero-contact-1" to address.property!!,
                "ero-contact-2" to address.street,
                "ero-contact-3" to address.town!!,
                "ero-contact-4" to address.area!!,
                "ero-contact-5" to address.postcode,
                "ero-contact-6" to emailAddress,
                "ero-contact-7" to phoneNumber,
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
                "ero-contact-1" to address.property!!,
                "ero-contact-2" to address.street,
                "ero-contact-3" to address.town!!,
                "ero-contact-4" to address.area!!,
                "ero-contact-5" to address.postcode,
                "ero-contact-6" to emailAddress,
                "ero-contact-7" to phoneNumber,
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
        verifyGeneratedPdfPlaceholders(contents, placeholders)
    }

    @Test
    fun `should create AED PDF with placeholders and image filled for English template`() {
        // Given
        val placeholders = mapOf(
            "electoral-number" to "GN422",
            "date-issued" to "20/04/2023",
            "certificate-number" to "TlbBclMIWfyQhaWxk0Zy",
        )
        val imageBytes = ResourceUtils.getFile(CERTIFICATE_SAMPLE_PHOTO).readBytes()
        val imageDetails =
            ImageDetails(absoluteX = 17.9f, absoluteY = 186.3f, fitWidth = 35f, fitHeight = 45f, bytes = imageBytes)
        val templateDetails = TemplateDetails(AED_PDF_TEMPLATE_ENGLISH, placeholders, listOf(imageDetails))

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        verifyGeneratedPdfPlaceholders(contents, placeholders)
        // FileOutputStream(File("Example_AED_English.pdf")).use { it.write(contents) }
    }

    @Test
    fun `should create AED PDF with placeholders and image filled for Welsh template`() {
        // Given
        val placeholders = mapOf(
            "electoral-number" to "KS7223",
            "date-issued" to "26/04/2023",
            "certificate-number" to "G1eQIZSYOhP7AeKnhJ8E",
        )
        val imageBytes = ResourceUtils.getFile(CERTIFICATE_SAMPLE_PHOTO).readBytes()
        val imageDetails =
            ImageDetails(absoluteX = 17.9f, absoluteY = 186.3f, fitWidth = 35f, fitHeight = 45f, bytes = imageBytes)
        val templateDetails = TemplateDetails(AED_PDF_TEMPLATE_WELSH, placeholders, listOf(imageDetails))

        // When
        val contents = pdfFactory.createPdfContents(templateDetails)

        // Then
        verifyGeneratedPdfPlaceholders(contents, placeholders)
        // FileOutputStream(File("Example_AED_Bilingual.pdf")).use { it.write(contents) }
    }

    private fun verifyGeneratedPdfPlaceholders(contents: ByteArray, placeholders: Map<String, String>) {
        PdfReader(contents).use { reader ->
            val text = PdfTextExtractor(reader).getTextFromPage(1)
            placeholders.values.forEach { assertThat(text).contains(it) }
        }
    }
}
