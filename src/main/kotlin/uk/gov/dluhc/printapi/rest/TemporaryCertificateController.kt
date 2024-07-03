package uk.gov.dluhc.printapi.rest

import mu.KotlinLogging
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
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.ResponseFileTooLargeException
import uk.gov.dluhc.printapi.mapper.GenerateTemporaryCertificateMapper
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateSummaryMapper
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.models.TemporaryCertificateSummariesResponse
import uk.gov.dluhc.printapi.service.S3PhotoService
import uk.gov.dluhc.printapi.service.StatisticsUpdateService
import uk.gov.dluhc.printapi.service.pdf.ExplainerPdfService
import uk.gov.dluhc.printapi.service.temporarycertificate.TemporaryCertificateService
import uk.gov.dluhc.printapi.service.temporarycertificate.TemporaryCertificateSummaryService
import java.io.ByteArrayInputStream
import javax.validation.Valid

private val logger = KotlinLogging.logger {}

@RestController
@CrossOrigin
class TemporaryCertificateController(
    private val temporaryCertificateSummaryService: TemporaryCertificateSummaryService,
    private val temporaryCertificateSummaryMapper: TemporaryCertificateSummaryMapper,
    @Qualifier("temporaryCertificateExplainerExplainerPdfService") private val explainerPdfService: ExplainerPdfService,
    private val temporaryCertificateService: TemporaryCertificateService,
    private val statisticsUpdateService: StatisticsUpdateService,
    private val s3Service: S3PhotoService,
    private val generateTemporaryCertificateMapper: GenerateTemporaryCertificateMapper,
) {

    companion object {
        // VAC cannot handle files of 10MB or more
        const val MAX_RESPONSE_FILE_SIZE = 9 * 1024 * 1024
    }

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
    fun generateTemporaryCertificate(
        @PathVariable eroId: String,
        @RequestBody @Valid generateTemporaryCertificateRequest: GenerateTemporaryCertificateRequest,
        authentication: Authentication
    ): ResponseEntity<InputStreamResource> {
        val userId = authentication.name
        val dto = generateTemporaryCertificateMapper.toGenerateTemporaryCertificateDto(
            generateTemporaryCertificateRequest,
            userId
        )
        return temporaryCertificateService.generateTemporaryCertificate(eroId, dto).also {
            if (it.contents.size > MAX_RESPONSE_FILE_SIZE && generateTemporaryCertificateRequest.allowLargeResponse != true) {
                val s3Path = "temporary_certificates/${dto.gssCode}/${dto.applicationReference}"
                logger.warn {
                    "Response file for eroId = $eroId and sourceReference = ${dto.sourceReference} too large " +
                        "to return Temporary VAC - putting to S3 path $s3Path instead"
                }
                s3Service.putObjectToTargetBucketFromByteArray(s3Path, it.contents)
                throw ResponseFileTooLargeException(
                    eroId,
                    "Temporary VAC",
                    dto.sourceReference
                )
            }
        }.also {
            statisticsUpdateService.triggerVoterCardStatisticsUpdate(dto.sourceReference)
        }.let { pdfFile ->
            ResponseEntity.status(CREATED)
                .headers(createPdfHttpHeaders(pdfFile))
                .body(InputStreamResource(ByteArrayInputStream(pdfFile.contents)))
        }
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
