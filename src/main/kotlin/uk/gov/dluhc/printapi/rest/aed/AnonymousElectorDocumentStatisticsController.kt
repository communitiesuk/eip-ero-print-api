package uk.gov.dluhc.printapi.rest.aed

import mu.KotlinLogging
import org.springframework.http.HttpStatus.OK
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException
import uk.gov.dluhc.printapi.models.CertificateStatisticsStatus
import uk.gov.dluhc.printapi.models.StatisticsResponse
import uk.gov.dluhc.printapi.service.aed.AnonymousElectorDocumentService

private val logger = KotlinLogging.logger {}

@RestController
@CrossOrigin
@RequestMapping("/anonymous-elector-documents/statistics")
class AnonymousElectorDocumentStatisticsController(
    private val anonymousElectorDocumentService: AnonymousElectorDocumentService,
) {
    @GetMapping
    @ResponseStatus(OK)
    fun getAnonymousElectorDocumentStatistics(@RequestParam applicationId: String): StatisticsResponse {
        logger.debug { "Collecting AED statistics for applicationId $applicationId" }

        val anonymousElectorDocuments = anonymousElectorDocumentService
            .getAnonymousElectorDocumentsByApplicationId(applicationId)

        if (anonymousElectorDocuments.isEmpty()) {
            throw CertificateNotFoundException(SourceType.ANONYMOUS_ELECTOR_DOCUMENT, applicationId)
        }

        return StatisticsResponse(
            certificateStatus = CertificateStatisticsStatus.DISPATCHED,
            certificateReprinted = anonymousElectorDocuments.size > 1,
            temporaryCertificateIssued = false
        )
    }
}
