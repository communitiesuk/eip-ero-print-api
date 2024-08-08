package uk.gov.dluhc.printapi.rest.aed

import org.springframework.http.HttpStatus.OK
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException
import uk.gov.dluhc.printapi.models.PreSignedUrlResourceResponse
import uk.gov.dluhc.printapi.rest.HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY
import uk.gov.dluhc.printapi.service.S3AccessService
import uk.gov.dluhc.printapi.service.aed.AnonymousElectorDocumentService

@RestController
@CrossOrigin
@RequestMapping("/eros/{eroId}/anonymous-elector-documents/photo")
class AnonymousElectorDocumentPhotoController(
    private val anonymousElectorDocumentService: AnonymousElectorDocumentService,
    private val s3AccessService: S3AccessService
) {

    @GetMapping
    @PreAuthorize(HAS_ERO_VC_ANONYMOUS_ADMIN_AUTHORITY)
    @ResponseStatus(OK)
    fun getAnonymousElectorDocumentsPhoto(
        @PathVariable eroId: String,
        @RequestParam applicationId: String,
    ): PreSignedUrlResourceResponse {
        val aed = anonymousElectorDocumentService
            .getAnonymousElectorDocuments(eroId, applicationId)
            .firstOrNull() ?: throw CertificateNotFoundException(eroId, ANONYMOUS_ELECTOR_DOCUMENT, applicationId)

        val preSignedUrl = s3AccessService.generatePresignedGetCertificatePhotoUrl(aed.photoLocationArn)
        return PreSignedUrlResourceResponse(preSignedUrl = preSignedUrl)
    }
}
