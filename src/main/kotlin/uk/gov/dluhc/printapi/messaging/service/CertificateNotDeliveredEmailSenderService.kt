package uk.gov.dluhc.printapi.messaging.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.dto.SendCertificateNotDeliveredEmailRequest
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage

private val logger = KotlinLogging.logger {}

@Service
class CertificateNotDeliveredEmailSenderService(
    electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient,
    private val emailService: EmailService,
) : AbstractEmailSender(electoralRegistrationOfficeManagementApiClient) {

    override fun send(printResponse: ProcessPrintResponseMessage, certificate: Certificate) {
        try {
            val request = with(certificate) {
                SendCertificateNotDeliveredEmailRequest(
                    sourceReference = sourceReference!!,
                    applicationReference = applicationReference!!,
                    localAuthorityEmailAddresses = setOf(getLocalAuthorityEmailAddress(gssCode!!)),
                    requestingUserEmailAddress = getRequestingUserEmailAddress(printRequests, printResponse.requestId)
                )
            }
            emailService.sendCertificateNotDeliveredEmail(request)
        } catch (e: Exception) {
            logger.error(
                "failed to send [Certificate Not Delivered] email when processing ProcessPrintResponseMessage for " +
                    "certificate [${certificate.id}] with requestId [${printResponse.requestId}]: ${e.message}"
            )
        }
    }
}
