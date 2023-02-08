package uk.gov.dluhc.printapi.service.temporarycertificate

import com.lowagie.text.Image
import com.lowagie.text.pdf.AcroFields
import com.lowagie.text.pdf.PdfReader
import com.lowagie.text.pdf.PdfStamper
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import java.io.ByteArrayOutputStream

@Component
class PdfFactory {

    fun createPdfContents(templateDetails: TemplateDetails): ByteArray {
        val outputStream = ByteArrayOutputStream()
        PdfReader(ResourceUtils.getFile(templateDetails.path).inputStream()).use { reader ->
            val stamper = PdfStamper(reader, outputStream)
            stamper.cleanMetadata()
            try {
                populateFormFields(stamper.acroFields, templateDetails.placeholders)
                addImages(templateDetails.images, stamper)
            } finally {
                stamper.close()
            }
        }
        return outputStream.toByteArray()
    }

    private fun addImages(images: List<ImageDetails>, stamper: PdfStamper) {
        if (images.isEmpty()) {
            return
        }
        images.forEach { addImageToCanvas(stamper, it) }
    }

    private fun addImageToCanvas(stamper: PdfStamper, imageDetails: ImageDetails) {
        val image = prepareImage(imageDetails)
        val canvas = stamper.getOverContent(imageDetails.pageNumber)
        canvas.addImage(image)
    }

    private fun prepareImage(imageDetails: ImageDetails): Image {
        val image = Image.getInstance(imageDetails.bytes)
        image.setAbsolutePosition(imageDetails.absoluteX, imageDetails.absoluteY)
        image.scaleToFit(imageDetails.fitWidth, imageDetails.fitHeight)
        return image
    }

    private fun populateFormFields(form: AcroFields, placeholders: Map<String, String>) {
        placeholders.forEach { (key, value) -> form.setField(key, value) }
    }
}
