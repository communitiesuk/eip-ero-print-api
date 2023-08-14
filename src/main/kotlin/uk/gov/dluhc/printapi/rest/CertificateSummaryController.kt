package uk.gov.dluhc.printapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.mapper.CertificateSummaryResponseMapper
import uk.gov.dluhc.printapi.mapper.VacSearchQueryStringParametersMapper
import uk.gov.dluhc.printapi.mapper.VacSummarySearchResponseMapper
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.models.VacSearchSummaryResponse
import uk.gov.dluhc.printapi.service.CertificateSummaryService
import uk.gov.dluhc.printapi.service.VacSummarySearchService
import javax.validation.Valid

@RestController
@CrossOrigin
class CertificateSummaryController(
    private val certificateSummaryService: CertificateSummaryService,
    private val certificateSearchSummaryService: VacSummarySearchService,
    private val certificateSummaryResponseMapper: CertificateSummaryResponseMapper,
    private val vacSummarySearchResponseMapper: VacSummarySearchResponseMapper,
    private val vacSearchQueryStringParametersMapper: VacSearchQueryStringParametersMapper
) {
    @GetMapping("/eros/{eroId}/certificates")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun getCertificateSummaryByApplicationId(
        @PathVariable eroId: String,
        @RequestParam applicationId: String,
    ): CertificateSummaryResponse {
        return certificateSummaryService.getCertificateSummary(eroId, VOTER_CARD, applicationId)
            .let { certificateSummaryResponseMapper.toCertificateSummaryResponse(it) }
    }

    @GetMapping("/eros/{eroId}/certificates/search")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun searchCertificates(
        @PathVariable eroId: String,
        @Valid searchQueryStringParameters: VacSearchQueryStringParameters
    ): VacSearchSummaryResponse {
        val searchCriteriaDto =
            vacSearchQueryStringParametersMapper.toVacSearchCriteriaDto(
                eroId = eroId,
                searchQueryParameters = searchQueryStringParameters
            )
        with(certificateSearchSummaryService.searchVacSummaries(searchCriteriaDto)) {
            return vacSummarySearchResponseMapper.toVacSearchSummaryResponse(this)
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
