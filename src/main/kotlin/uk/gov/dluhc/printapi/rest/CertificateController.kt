package uk.gov.dluhc.printapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.models.CertificateSummaryResponse

@RestController
@CrossOrigin
class CertificateController {
    companion object {
        const val ERO_VC_ADMIN_GROUP_PREFIX = "ero-vc-admin-"
    }

    @GetMapping("/eros/{eroId}/certificates/applications/{applicationId}")
    @PreAuthorize(
        """
        hasAnyAuthority(
            T(uk.gov.dluhc.printapi.rest.CertificateController).ERO_VC_ADMIN_GROUP_PREFIX.concat(#eroId)
        )
        """
    )
    fun getCertificateSummaryByApplicationId(
        @PathVariable eroId: String,
        @PathVariable applicationId: String,
    ): CertificateSummaryResponse {
        TODO("Will be implemented as part of EIP1-2597")
    }
}
