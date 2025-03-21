package uk.gov.dluhc.printapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.CertificateMapper
import uk.gov.dluhc.printapi.mapper.PrintRequestMapper
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient as EroClient

@Service
class PrintService(
    private val eroClient: EroClient,
    private val sourceTypeMapper: SourceTypeMapper,
    private val certificateMapper: CertificateMapper,
    private val printRequestMapper: PrintRequestMapper,
    private val certificateRepository: CertificateRepository
) {
    @Transactional
    fun savePrintMessage(message: SendApplicationToPrintMessage): Certificate {
        val ero = eroClient.getEro(message.gssCode!!)

        val certificate = certificateRepository.findByGssCodeInAndSourceTypeAndSourceReference(
            gssCodes = listOf(message.gssCode),
            sourceType = sourceTypeMapper.mapSqsToEntity(message.sourceType),
            sourceReference = message.sourceReference
        )?.also {
            it.addPrintRequest(printRequestMapper.toPrintRequest(message, ero))
        } ?: run { certificateMapper.toCertificate(message, ero) }

        return certificateRepository.save(certificate)
    }
}
