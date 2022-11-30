package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.mapper.CertificateMapper
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import javax.transaction.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient as EroClient

@Service
class PrintService(
    private val eroClient: EroClient,
    private val certificateMapper: CertificateMapper,
    private val certificateRepository: CertificateRepository
) {
    @Transactional
    fun savePrintMessage(message: SendApplicationToPrintMessage) {
        val ero = eroClient.getEro(message.gssCode!!)
        val certificate = certificateMapper.toCertificate(message, ero)
        certificateRepository.save(certificate)
    }
}
