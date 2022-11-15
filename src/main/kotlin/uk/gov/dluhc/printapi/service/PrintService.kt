package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.rds.mapper.CertificateMapper
import uk.gov.dluhc.printapi.rds.repository.CertificateRepository
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
        val ero = eroClient.getElectoralRegistrationOffice(message.gssCode!!)
        val localAuthority = ero.localAuthorities.first { it.gssCode == message.gssCode }
        val certificate = certificateMapper.toCertificate(message, ero, localAuthority.name)
        certificateRepository.save(certificate)
    }
}
