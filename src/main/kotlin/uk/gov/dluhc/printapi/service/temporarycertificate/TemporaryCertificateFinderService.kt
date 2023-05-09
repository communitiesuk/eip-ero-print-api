package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.service.EroService

@Service
class TemporaryCertificateFinderService(
    private val eroService: EroService,
    private val temporaryCertificateRepository: TemporaryCertificateRepository
) {

    fun getTemporaryCertificates(eroId: String, sourceType: SourceType, sourceReference: String): List<TemporaryCertificate> =
        eroService.lookupGssCodesForEro(eroId).let { gssCodes ->
            temporaryCertificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(gssCodes, sourceType, sourceReference)
        }
}
