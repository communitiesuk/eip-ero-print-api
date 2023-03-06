package uk.gov.dluhc.printapi.rest

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.mapper.GenerateAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.service.aed.AnonymousElectorDocumentService
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfService
import java.io.ByteArrayInputStream
import javax.validation.Valid

@RestController
@CrossOrigin
@RequestMapping("/eros/{eroId}/anonymous-elector-documents")
class AnonymousElectorDocumentController(
    @Qualifier("anonymousElectorDocumentExplainerPdfService") private val explainerPdfService: ExplainerPdfService,
    private val generateAnonymousElectorDocumentMapper: GenerateAnonymousElectorDocumentMapper,
    private val anonymousElectorDocumentService: AnonymousElectorDocumentService,
) {

    @PostMapping
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    fun generateAnonymousElectorDocument(
        @PathVariable eroId: String,
        @RequestBody @Valid generateAnonymousElectorDocumentRequest: GenerateAnonymousElectorDocumentRequest,
        authentication: Authentication
    ): ResponseEntity<InputStreamResource> {
        val userId = authentication.name
        val dto = generateAnonymousElectorDocumentMapper.toGenerateAnonymousElectorDocumentDto(
            generateAnonymousElectorDocumentRequest,
            userId
        )
        return anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, dto).let { pdfFile ->
            ResponseEntity.status(CREATED)
                .headers(createPdfHttpHeaders(pdfFile))
                .body(InputStreamResource(ByteArrayInputStream(pdfFile.contents)))
        }
    }

    @PatchMapping("{documentNumber}/status")
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateAnonymousElectorDocumentStatus(@PathVariable eroId: String, @PathVariable documentNumber: String) {
        TODO("Not yet implemented")
    }

    @PostMapping(value = ["{gssCode}/explainer-document"], produces = [APPLICATION_PDF_VALUE])
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    fun generateAedExplainerDocument(
        @PathVariable eroId: String,
        @PathVariable gssCode: String,
    ): ResponseEntity<InputStreamResource> {
        return explainerPdfService.generateExplainerPdf(eroId, gssCode).let { pdfFile ->
            ResponseEntity.status(CREATED)
                .headers(createPdfHttpHeaders(pdfFile))
                .body(InputStreamResource(ByteArrayInputStream(pdfFile.contents)))
        }
    }

    private fun createPdfHttpHeaders(pdfFile: PdfFile): HttpHeaders {
        val headers = HttpHeaders()
        headers.contentType = APPLICATION_PDF
        headers.add(CONTENT_DISPOSITION, "inline; filename=${pdfFile.filename}")
        return headers
    }
}
