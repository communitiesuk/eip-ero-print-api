package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException

private val logger = KotlinLogging.logger {}

@Service
class CertificateFinderService(
    private val eroService: EroService,
    private val certificateRepository: CertificateRepository
) {

    fun getCertificate(eroId: String, sourceType: SourceType, sourceReference: String) =
        eroService.lookupGssCodesForEro(eroId).let { gssCodes ->
            certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, sourceType, sourceReference)
                ?: throw CertificateNotFoundException(eroId, sourceType, sourceReference)
                    .also { logger.warn(it.message) }
        }

    fun getCertificate(sourceType: SourceType, sourceReference: String) =
        certificateRepository.findBySourceTypeAndSourceReference(sourceType, sourceReference)
            ?: throw CertificateNotFoundException(sourceType, sourceReference)
                .also {
                    logger.warn(it.message)
                }
}
