package uk.gov.dluhc.printapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class TemporaryCertificateController {

    @PostMapping("/eros/{eroId}/temporary-certificate")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun generateTemporaryCertificate(@PathVariable eroId: String) {
        TODO("not yet implemented")
    }
}
