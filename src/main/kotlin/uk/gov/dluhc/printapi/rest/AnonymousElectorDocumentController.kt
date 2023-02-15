package uk.gov.dluhc.printapi.rest

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfService
import java.io.ByteArrayInputStream

@RestController
@CrossOrigin
class AnonymousElectorDocumentController(
    @Qualifier("anonymousElectorDocumentExplainerExplainerPdfService") private val explainerPdfService: ExplainerPdfService,
) {

    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    @PostMapping(
        value = ["/eros/{eroId}/anonymous-elector-documents/{gssCode}/explainer-document"],
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    fun generateAedExplainerDocument(
        @PathVariable eroId: String,
        @PathVariable gssCode: String,
    ): ResponseEntity<InputStreamResource> {
        return explainerPdfService.generateExplainerPdf(eroId, gssCode).let { pdfFile ->
            ResponseEntity.status(HttpStatus.CREATED)
                .headers(createPdfHttpHeaders(pdfFile))
                .body(InputStreamResource(ByteArrayInputStream(pdfFile.contents)))
        }
    }

    private fun createPdfHttpHeaders(pdfFile: PdfFile): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.add("Content-Disposition", "inline; filename=${pdfFile.filename}")
        return headers
    }
}
