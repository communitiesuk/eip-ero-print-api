package uk.gov.dluhc.printapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.database.mapper.CertificateSummaryResponseMapper
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse
import uk.gov.dluhc.printapi.service.CertificateSummaryService

@RestController
@CrossOrigin
class CertificateSummaryController(
    private val certificateSummaryService: CertificateSummaryService,
    private val certificateSummaryResponseMapper: CertificateSummaryResponseMapper
) {
    companion object {
        const val ERO_VC_ADMIN_GROUP_PREFIX = "ero-vc-admin-"
    }

    @GetMapping("/eros/{eroId}/certificates/applications/{applicationId}")
    @PreAuthorize(
        """
        hasAnyAuthority(
            T(uk.gov.dluhc.printapi.rest.CertificateSummaryController).ERO_VC_ADMIN_GROUP_PREFIX.concat(#eroId)
        )
        """
    )
    fun getCertificateSummaryByApplicationId(
        @PathVariable eroId: String,
        @PathVariable applicationId: String,
    ): CertificateSummaryResponse {
        return certificateSummaryService.getCertificateSummary(eroId, VOTER_CARD, applicationId)
            .let { certificateSummaryResponseMapper.toCertificateSummaryResponse(it) }
    }
}
