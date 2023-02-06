package uk.gov.dluhc.printapi.service.temporarycertificate

import com.lowagie.text.pdf.AcroFields
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import uk.gov.dluhc.printapi.dto.EroDto
import java.io.ByteArrayOutputStream

@Component
class ExplainerPdfFactory(
    private val explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory
) {

    fun createPdfContents(eroDetails: EroDto, gssCode: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val templateDetails = explainerPdfTemplateDetailsFactory.getTemplateDetails(gssCode, eroDetails)
        PdfReader(ResourceUtils.getFile(templateDetails.path).inputStream()).use { reader ->
            val stamper = PdfStamper(reader, outputStream)
            stamper.cleanMetadata()
            try {
                populateFormFields(stamper.acroFields, templateDetails.placeholders)
            } finally {
                stamper.close()
            }
        }
        return outputStream.toByteArray()
    }

    private fun populateFormFields(form: AcroFields, placeholders: Map<String, String>) {
        placeholders.forEach { (key, value) -> form.setField(key, value) }
    }
}
