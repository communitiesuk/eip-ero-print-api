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

    companion object {
        private const val POINTS_PER_INCH = 72f
        private const val MM_PER_INCH = 25.4f
    }

    fun createPdfContents(templateDetails: TemplateDetails): ByteArray {
        val outputStream = ByteArrayOutputStream()
        PdfReader(ResourceUtils.getFile(templateDetails.path).inputStream()).use { reader ->
            val stamper = PdfStamper(reader, outputStream)
            stamper.cleanMetadata()
            stamper.setFormFlattening(true)
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
        // OpenPDF positions the top left corner of the image in relation to an origin in the bottom left corner (0,0).
        // Measurements are expressed in Points where 1 point is equal to 1/72 of an inch or 0.352777 mm.
        // To simplify configuration, the top left corner or the image and its height and width are expressed in mm
        image.setAbsolutePosition(mmToPoints(imageDetails.absoluteX), mmToPoints(imageDetails.absoluteY))
        image.scaleToFit(mmToPoints(imageDetails.fitWidth), mmToPoints(imageDetails.fitHeight))
        return image
    }

    fun mmToPoints(mm: Float): Float {
        return mm * POINTS_PER_INCH / MM_PER_INCH
    }

    private fun populateFormFields(form: AcroFields, placeholders: Map<String, String>) {
        placeholders.forEach { (key, value) -> form.setField(key, value) }
    }
}
