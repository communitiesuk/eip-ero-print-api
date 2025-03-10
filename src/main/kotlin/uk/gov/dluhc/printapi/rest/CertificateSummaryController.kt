package uk.gov.dluhc.printapi.rest

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.mapper.CertificateSearchQueryStringParametersMapper
import uk.gov.dluhc.printapi.mapper.CertificateSummaryResponseMapper
import uk.gov.dluhc.printapi.mapper.CertificateSummarySearchResponseMapper
import uk.gov.dluhc.printapi.models.CertificateSearchSummaryResponse
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.service.CertificateSummarySearchService
import uk.gov.dluhc.printapi.service.CertificateSummaryService
import uk.gov.dluhc.printapi.service.S3AccessService

@RestController
@CrossOrigin
class CertificateSummaryController(
    private val certificateSummaryService: CertificateSummaryService,
    private val certificateSearchSummaryService: CertificateSummarySearchService,
    private val s3AccessService: S3AccessService,
    private val certificateSummaryResponseMapper: CertificateSummaryResponseMapper,
    private val certificateSummarySearchResponseMapper: CertificateSummarySearchResponseMapper,
    private val certificateSearchQueryStringParametersMapper: CertificateSearchQueryStringParametersMapper
) {
    @GetMapping("/eros/{eroId}/certificates")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun getCertificateSummaryByApplicationId(
        @PathVariable eroId: String,
        @RequestParam applicationId: String,
    ): CertificateSummaryResponse {
        return certificateSummaryService.getCertificateSummary(eroId, VOTER_CARD, applicationId)
            .let { certificateSummaryResponseMapper.toCertificateSummaryResponse(it, eroId) }
    }

    @GetMapping("/eros/{eroId}/certificates/photo")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun getCertificatePhoto(
        @PathVariable eroId: String,
        @RequestParam applicationId: String,
    ): PreSignedUrlResourceResponse {
        val certificate = certificateSummaryService.getCertificateSummary(eroId, VOTER_CARD, applicationId)
        val preSignedUrl = s3AccessService.generatePresignedGetCertificatePhotoUrl(certificate.photoLocationArn)
        return PreSignedUrlResourceResponse(preSignedUrl = preSignedUrl)
    }

    @GetMapping("/eros/{eroId}/certificates/search")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun searchCertificates(
        @PathVariable eroId: String,
        @Valid searchQueryStringParameters: CertificateSearchQueryStringParameters
    ): CertificateSearchSummaryResponse {
        val searchCriteriaDto =
            certificateSearchQueryStringParametersMapper.toCertificateSearchCriteriaDto(
                eroId = eroId,
                searchQueryParameters = searchQueryStringParameters
            )
        with(certificateSearchSummaryService.searchCertificateSummaries(searchCriteriaDto)) {
            return certificateSummarySearchResponseMapper.toCertificateSearchSummaryResponse(this, eroId)
        }
    }

    @Deprecated(
        "Use /eros/{eroId}/certificates?applicationId={applicationId} instead",
        ReplaceWith("getCertificateSummaryByApplicationId(eroId, applicationId)")
    )
    @GetMapping("/eros/{eroId}/certificates/applications/{applicationId}")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun deprecatedGetCertificateSummaryByApplicationId(
        @PathVariable eroId: String,
        @PathVariable applicationId: String,
    ): CertificateSummaryResponse =
        getCertificateSummaryByApplicationId(eroId, applicationId)
}
