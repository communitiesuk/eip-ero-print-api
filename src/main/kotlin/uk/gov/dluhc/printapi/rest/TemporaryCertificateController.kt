package uk.gov.dluhc.printapi.rest

import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.CONTENT_DISPOSITION
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.http.MediaType.APPLICATION_PDF_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.mapper.GenerateTemporaryCertificateMapper
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateSummaryMapper
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.models.TemporaryCertificateSummariesResponse
import uk.gov.dluhc.printapi.service.StatisticsUpdateService
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfService
import uk.gov.dluhc.printapi.service.temporarycertificate.TemporaryCertificateService
import uk.gov.dluhc.printapi.service.temporarycertificate.TemporaryCertificateSummaryService
import java.io.ByteArrayInputStream

@RestController
@CrossOrigin
class TemporaryCertificateController(
    private val temporaryCertificateSummaryService: TemporaryCertificateSummaryService,
    private val temporaryCertificateSummaryMapper: TemporaryCertificateSummaryMapper,
    @Qualifier("temporaryCertificateExplainerExplainerPdfService") private val explainerPdfService: ExplainerPdfService,
    private val temporaryCertificateService: TemporaryCertificateService,
    private val statisticsUpdateService: StatisticsUpdateService,
    private val generateTemporaryCertificateMapper: GenerateTemporaryCertificateMapper,
) {

    @GetMapping("/eros/{eroId}/temporary-certificates")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun getTemporaryCertificateSummariesByApplicationId(
        @PathVariable eroId: String,
        @RequestParam applicationId: String,
    ): TemporaryCertificateSummariesResponse =
        TemporaryCertificateSummariesResponse(
            temporaryCertificateSummaryService.getTemporaryCertificateSummaries(
                eroId,
                SourceType.VOTER_CARD,
                applicationId
            ).map {
                temporaryCertificateSummaryMapper.toApiTemporaryCertificateSummary(it)
            }.sortedByDescending {
                it.dateTimeGenerated
            }
        )

    @Deprecated(
        "Use /eros/{eroId}/temporary-certificates?applicationId={applicationId} instead",
        ReplaceWith("getTemporaryCertificateSummariesByApplicationId(eroId, applicationId)")
    )
    @GetMapping("/eros/{eroId}/temporary-certificates/applications/{applicationId}")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun deprecatedGetTemporaryCertificateSummariesByApplicationId(
        @PathVariable eroId: String,
        @PathVariable applicationId: String,
    ): TemporaryCertificateSummariesResponse =
        getTemporaryCertificateSummariesByApplicationId(eroId, applicationId)

    @PostMapping("/eros/{eroId}/temporary-certificates")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    @ResponseStatus(CREATED)
    fun generateTemporaryCertificate(
        @PathVariable eroId: String,
        @RequestBody @Valid generateTemporaryCertificateRequest: GenerateTemporaryCertificateRequest,
        authentication: Authentication
    ): PreSignedUrlResourceResponse {
        val userId = authentication.name
        val dto = generateTemporaryCertificateMapper.toGenerateTemporaryCertificateDto(
            generateTemporaryCertificateRequest,
            userId
        )
        return temporaryCertificateService.generateTemporaryCertificate(eroId, dto)
            .also {
                statisticsUpdateService.triggerApplicationStatisticsUpdate(dto.sourceReference)
            }.let { presignedUrl -> PreSignedUrlResourceResponse(presignedUrl) }
    }

    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    @PostMapping(
        value = ["/eros/{eroId}/temporary-certificates/{gssCode}/explainer-document"],
        produces = [APPLICATION_PDF_VALUE]
    )
    fun generateTempCertExplainerPdf(
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
