package uk.gov.dluhc.printapi.rest

import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.mapper.CertificateStatisticsStatusMapper
import uk.gov.dluhc.printapi.models.StatisticsResponse
import uk.gov.dluhc.printapi.service.CertificateFinderService
import uk.gov.dluhc.printapi.service.temporarycertificate.TemporaryCertificateFinderService

private val logger = KotlinLogging.logger {}

@RestController
@CrossOrigin
@RequestMapping("/certificates/statistics")
class CertificateStatisticsController(
    private val certificateFinderService: CertificateFinderService,
    private val temporaryCertificateFinderService: TemporaryCertificateFinderService,
    private val statusMapper: CertificateStatisticsStatusMapper,
) {
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    fun getCertificateStatistics(@RequestParam applicationId: String): StatisticsResponse {
        logger.debug { "Collecting VAC statistics for applicationId $applicationId" }

        val certificate = certificateFinderService.getCertificate(SourceType.VOTER_CARD, applicationId)
        val temporaryCertificates = temporaryCertificateFinderService.getTemporaryCertificates(
            SourceType.VOTER_CARD,
            applicationId,
        )

        return StatisticsResponse(
            certificateStatus = statusMapper.fromEntityPrintRequestStatus(certificate.status!!),
            certificateReprinted = certificate.printRequests.size > 1,
            temporaryCertificateIssued = temporaryCertificates.isNotEmpty()
        )
    }
}
