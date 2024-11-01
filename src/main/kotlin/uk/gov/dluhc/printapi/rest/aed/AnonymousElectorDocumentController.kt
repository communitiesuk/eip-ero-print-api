package uk.gov.dluhc.printapi.rest.aed

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.UpdateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.mapper.aed.AedSearchQueryStringParametersMapper
import uk.gov.dluhc.printapi.mapper.aed.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.AnonymousSearchSummaryMapper
import uk.gov.dluhc.printapi.mapper.aed.GenerateAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.ReIssueAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.UpdateAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.models.AedSearchSummaryResponse
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentsResponse
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.models.ReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.models.UpdateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.rest.HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY
import uk.gov.dluhc.printapi.service.aed.AnonymousElectorDocumentSearchService
import uk.gov.dluhc.printapi.service.aed.AnonymousElectorDocumentService
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfService
import java.beans.PropertyEditorSupport
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@CrossOrigin
@RequestMapping("/eros/{eroId}/anonymous-elector-documents")
class AnonymousElectorDocumentController(
    @Qualifier("anonymousElectorDocumentExplainerPdfService")
    private val explainerPdfService: ExplainerPdfService,
    private val anonymousElectorDocumentService: AnonymousElectorDocumentService,
    private val anonymousElectorDocumentSearchService: AnonymousElectorDocumentSearchService,
    private val generateAnonymousElectorDocumentMapper: GenerateAnonymousElectorDocumentMapper,
    private val reIssueAnonymousElectorDocumentMapper: ReIssueAnonymousElectorDocumentMapper,
    private val anonymousElectorDocumentMapper: AnonymousElectorDocumentMapper,
    private val anonymousSearchSummaryMapper: AnonymousSearchSummaryMapper,
    private val aedSearchQueryStringParametersMapper: AedSearchQueryStringParametersMapper,
    private val updateAnonymousElectorDocumentMapper: UpdateAnonymousElectorDocumentMapper
) {

    @InitBinder
    fun registerQueryStringParameterPropertyEditor(dataBinder: WebDataBinder) {
        dataBinder.registerCustomEditor(String::class.java, QueryStringParameterPropertyEditor())
    }

    @PostMapping
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    fun generateAnonymousElectorDocument(
        @PathVariable eroId: String,
        @RequestBody @Valid generateAnonymousElectorDocumentRequest: GenerateAnonymousElectorDocumentRequest,
        authentication: Authentication
    ): ResponseEntity<InputStreamResource> {
        val dto = generateAnonymousElectorDocumentMapper.toGenerateAnonymousElectorDocumentDto(
            apiRequest = generateAnonymousElectorDocumentRequest,
            userId = authentication.name
        )
        return anonymousElectorDocumentService.generateAnonymousElectorDocument(eroId, dto).let { pdfFile ->
            ResponseEntity.status(CREATED)
                .headers(createPdfHttpHeaders(pdfFile))
                .body(InputStreamResource(ByteArrayInputStream(pdfFile.contents)))
        }
    }

    @PostMapping("/re-issue")
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    fun reIssueAnonymousElectorDocument(
        @PathVariable eroId: String,
        @RequestBody @Valid reIssueAnonymousElectorDocumentRequest: ReIssueAnonymousElectorDocumentRequest,
        authentication: Authentication
    ): ResponseEntity<InputStreamResource> {
        val dto = reIssueAnonymousElectorDocumentMapper.toReIssueAnonymousElectorDocumentDto(
            apiRequest = reIssueAnonymousElectorDocumentRequest,
            userId = authentication.name
        )
        return anonymousElectorDocumentService.reIssueAnonymousElectorDocument(eroId, dto).let { pdfFile ->
            ResponseEntity.status(CREATED)
                .headers(createPdfHttpHeaders(pdfFile))
                .body(InputStreamResource(ByteArrayInputStream(pdfFile.contents)))
        }
    }

    @PatchMapping
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    @ResponseStatus(NO_CONTENT)
    fun updateAnonymousElectorDocumentByApplicationId(
        @PathVariable eroId: String,
        @RequestBody @Valid updateRequest: UpdateAnonymousElectorDocumentRequest,
        authentication: Authentication
    ) {
        updateRequest.validate()
        anonymousElectorDocumentService.updateFullyRetainedAnonymousElectorDocuments(
            eroId, updateAnonymousElectorDocumentMapper.toUpdateAedDto(updateRequest)
        )
    }

    @GetMapping
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    @ResponseStatus(OK)
    fun getAnonymousElectorDocuments(
        @PathVariable eroId: String,
        @RequestParam applicationId: String,
    ): AnonymousElectorDocumentsResponse {
        val anonymousElectorDocuments = anonymousElectorDocumentService
            .getFullyRetainedAnonymousElectorDocuments(eroId, applicationId)
            .map { anonymousElectorDocumentMapper.mapToApiAnonymousElectorDocument(it, eroId) }
        return AnonymousElectorDocumentsResponse(anonymousElectorDocuments = anonymousElectorDocuments)
    }

    @GetMapping("/search")
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    @ResponseStatus(OK)
    fun searchAnonymousElectorDocumentSummaries(
        @PathVariable eroId: String,
        @Valid searchQueryStringParameters: AedSearchQueryStringParameters
    ): AedSearchSummaryResponse {
        logger.debug { "Searching AED summaries for eroId [$eroId] with searchQueryStringParameters [$searchQueryStringParameters]" }
        val searchCriteriaDto =
            aedSearchQueryStringParametersMapper.toAnonymousSearchCriteriaDto(
                eroId = eroId,
                searchQueryParameters = searchQueryStringParameters
            )
        with(anonymousElectorDocumentSearchService.searchAnonymousElectorDocumentSummaries(searchCriteriaDto)) {
            return anonymousSearchSummaryMapper.toAedSearchSummaryResponse(this)
        }
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

/**
 * Property Editor that URL decodes strings, then does a full trim, and returns null for resultant empty strings.
 */
class QueryStringParameterPropertyEditor : PropertyEditorSupport() {
    override fun setAsText(text: String?) {
        value = text
            ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }?.trim()
            ?.let { it.ifEmpty { null } }
    }
}

private fun UpdateAnonymousElectorDocumentRequest.validate() {
    if (email.isNullOrBlank() && phoneNumber.isNullOrBlank()) {
        throw UpdateAnonymousElectorDocumentValidationException("Either email or phoneNumber must be provided")
    }
}
