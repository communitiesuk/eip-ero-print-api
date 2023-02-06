package uk.gov.dluhc.printapi.rest

import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.service.temporarycertificate.ExplainerPdfService
import java.io.ByteArrayInputStream
import javax.validation.Valid

@RestController
@CrossOrigin
class TemporaryCertificateController(
    val explainerPdfService: ExplainerPdfService
) {

    @PostMapping("/eros/{eroId}/temporary-certificate")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun generateTemporaryCertificate(
        @PathVariable eroId: String,
        @RequestBody @Valid generateTemporaryCertificateRequest: GenerateTemporaryCertificateRequest,
        authentication: Authentication
    ) {
        val userId = authentication.name
        TODO("not yet implemented")
    }

    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    @PostMapping(
        value = ["/eros/{eroId}/temporary-certificate/{gssCode}/explainer-document"],
        produces = [MediaType.APPLICATION_PDF_VALUE]
    )
    fun generateTempCertExplainerPdf(
        @PathVariable eroId: String,
        @PathVariable gssCode: String,
    ): ResponseEntity<InputStreamResource> {
        return explainerPdfService.generateExplainerPdf(gssCode).let { pdfFile ->
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
