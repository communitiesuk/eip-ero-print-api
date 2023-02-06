package uk.gov.dluhc.printapi.rest

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.InitBinder
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.validator.GenerateTemporaryCertificateRequestValidator
import javax.validation.Valid

@RestController
@CrossOrigin
class TemporaryCertificateController(
    private val generateTemporaryCertificateRequestValidator: GenerateTemporaryCertificateRequestValidator
) {

    @InitBinder("generateTemporaryCertificateRequest")
    fun initGenerateTemporaryCertificateRequestValidator(binder: WebDataBinder) {
        binder.addValidators(generateTemporaryCertificateRequestValidator)
    }

    @PostMapping("/eros/{eroId}/temporary-certificate")
    @PreAuthorize(HAS_ERO_VC_ADMIN_AUTHORITY)
    fun generateTemporaryCertificate(
        @PathVariable eroId: String,
        @RequestBody @Valid generateTemporaryCertificateRequest: GenerateTemporaryCertificateRequest,
        authentication: Authentication
    ) {
        val userId = authentication.name
        TODO("not yet implemented")
    }
}
