package uk.gov.dluhc.printapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.mapper.CertificateSummaryResponseMapper
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.service.CertificateSummaryService

@RestController
@CrossOrigin
class CertificateSummaryController(
    private val certificateSummaryService: CertificateSummaryService,
    private val certificateSummaryResponseMapper: CertificateSummaryResponseMapper
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

    @Deprecated("Use /eros/{eroId}/certificates?applicationId={applicationId} instead")
    @GetMapping("/eros/{eroId}/certificates/applications/{applicationId}")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun deprecatedGetCertificateSummaryByApplicationId(
        @PathVariable eroId: String,
        @PathVariable applicationId: String,
    ): CertificateSummaryResponse =
        getCertificateSummaryByApplicationId(eroId, applicationId)
}
