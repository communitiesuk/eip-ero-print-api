package uk.gov.dluhc.printapi.service.tempcert

import com.lowagie.text.pdf.AcroFields
import com.lowagie.text.pdf.PdfReader
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto

@ExtendWith(MockitoExtension::class)
internal class ExplainerPdfFactoryTest {

    companion object {
        private const val PDF_TEMPLATE_ENGLISH = "temp-certs-templates/Explainer Document (English).pdf"
        private const val PDF_TEMPLATE_WELSH = "temp-certs-templates/Explainer Document (Dual Language).pdf"
    }

    @Mock
    private lateinit var explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory

    @InjectMocks
    private lateinit var explainerPdfFactory: ExplainerPdfFactory

    @Test
    fun `should create PDF with placeholders filled for English template`() {
        // Given
        val eroDetails = buildEroDto()
        val gssCode = aGssCode()
        val placeholders = with(eroDetails.englishContactDetails) {
            mapOf(
                "eroName" to name,
                "eroAddressLine1" to address.property.orEmpty(),
                "eroAddressLine2" to address.street,
                "eroAddressLine3" to address.town.orEmpty(),
                "eroAddressLine4" to address.area.orEmpty(),
                "eroPostcode" to address.postcode,
                "eroEmailAddress" to emailAddress,
                "eroPhoneNumber" to phoneNumber,
            )
        }
        val templateDetails = TemplateDetails(PDF_TEMPLATE_ENGLISH, placeholders)
        given(explainerPdfTemplateDetailsFactory.getTemplateDetails(any(), any())).willReturn(templateDetails)

        // When
        val contents = explainerPdfFactory.createPdfContents(eroDetails, gssCode)

        // Then
        verify(explainerPdfTemplateDetailsFactory).getTemplateDetails(gssCode, eroDetails)
        // FileOutputStream(File("Example_English.pdf")).use { it.write(contents) }
        PdfReader(contents).use { reader ->
            verifyEnglishPlaceholders(reader.acroFields, placeholders)
        }
    }

    @Test
    fun `should create PDF with placeholders filled for Welsh template`() {
        // Given
        val eroDetails = buildEroDto()
        val gssCode = aGssCode()
        val placeholders = with(eroDetails.welshContactDetails!!) {
            mapOf(
                "eroName" to name,
                "eroAddressLine1" to address.property.orEmpty(),
                "eroAddressLine2" to address.street,
                "eroAddressLine3" to address.town.orEmpty(),
                "eroAddressLine4" to address.area.orEmpty(),
                "eroAddressPostcode" to address.postcode,
                "eroEmailAddress" to emailAddress,
                "eroPhoneNumber" to phoneNumber,
            )
        }
        val templateDetails = TemplateDetails(PDF_TEMPLATE_WELSH, placeholders)
        given(explainerPdfTemplateDetailsFactory.getTemplateDetails(any(), any())).willReturn(templateDetails)

        // When
        val contents = explainerPdfFactory.createPdfContents(eroDetails, gssCode)

        // Then
        verify(explainerPdfTemplateDetailsFactory).getTemplateDetails(gssCode, eroDetails)
        // FileOutputStream(File("Example_Welsh.pdf")).use { it.write(contents) }
        PdfReader(contents).use { reader ->
            verifyWelshPlaceholders(reader.acroFields, placeholders)
        }
    }

    private fun verifyEnglishPlaceholders(form: AcroFields, placeholders: Map<String, String>) {
        verifyPlaceholder(form, placeholders, "eroName")
        verifyPlaceholder(form, placeholders, "eroAddressLine1")
        verifyPlaceholder(form, placeholders, "eroAddressLine2")
        verifyPlaceholder(form, placeholders, "eroAddressLine3")
        verifyPlaceholder(form, placeholders, "eroAddressLine4")
        verifyPlaceholder(form, placeholders, "eroPostcode")
        verifyPlaceholder(form, placeholders, "eroEmailAddress")
        verifyPlaceholder(form, placeholders, "eroPhoneNumber")
    }

    private fun verifyWelshPlaceholders(form: AcroFields, placeholders: Map<String, String>) {
        verifyPlaceholder(form, placeholders, "eroName")
        verifyPlaceholder(form, placeholders, "eroAddressLine1")
        verifyPlaceholder(form, placeholders, "eroAddressLine2")
        verifyPlaceholder(form, placeholders, "eroAddressLine3")
        verifyPlaceholder(form, placeholders, "eroAddressLine4")
        verifyPlaceholder(form, placeholders, "eroAddressPostcode")
        verifyPlaceholder(form, placeholders, "eroEmailAddress")
        verifyPlaceholder(form, placeholders, "eroPhoneNumber")
    }

    private fun verifyPlaceholder(form: AcroFields, placeholders: Map<String, String>, name: String) {
        assertThat(form.getField(name)).isEqualTo(placeholders[name])
    }
}
