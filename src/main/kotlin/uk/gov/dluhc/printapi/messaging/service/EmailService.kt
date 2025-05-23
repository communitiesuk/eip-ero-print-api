package uk.gov.dluhc.printapi.messaging.service

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.apache.commons.text.StringSubstitutor.replace
import org.springframework.stereotype.Service
import uk.gov.dluhc.email.EmailClient
import uk.gov.dluhc.printapi.config.EmailContentConfiguration
import uk.gov.dluhc.printapi.config.EmailContentProperties
import uk.gov.dluhc.printapi.dto.SendCertificateFailedToPrintEmailRequest
import uk.gov.dluhc.printapi.dto.SendCertificateNotDeliveredEmailRequest
import uk.gov.dluhc.printapi.dto.SendEmailRequest

private val logger = KotlinLogging.logger {}

@Service
class EmailService(
    private val emailClient: EmailClient,
    private val emailContentConfiguration: EmailContentConfiguration,
) {
    fun sendCertificateNotDeliveredEmail(request: SendCertificateNotDeliveredEmailRequest) {
        logger.info("sending [Certificate Not Delivered] email to [${request.localAuthorityEmailAddresses}]")
        val emailConfig = emailContentConfiguration.certificateReturned
        sendEmail(request, emailConfig)
    }

    fun sendCertificateFailedToPrintEmail(request: SendCertificateFailedToPrintEmailRequest) {
        logger.info("sending [Certificate Failed To Print] email to [${request.localAuthorityEmailAddresses}]")
        val emailConfig = emailContentConfiguration.certificateFailedToPrint
        sendEmail(request, emailConfig)
    }

    private fun sendEmail(
        request: SendEmailRequest,
        emailConfig: EmailContentProperties
    ) {
        val substitutionVariables = with(request) {
            mapOf(
                "sourceReference" to sourceReference,
                "applicationReference" to applicationReference,
                "localAuthorityEmailAddresses" to localAuthorityEmailAddresses,
                "applicationUrl" to "${emailContentConfiguration.vacBaseUrl}/${request.sourceReference}"
            )
        }

        with(emailConfig) {
            emailClient.send(
                emailToRecipients = request.localAuthorityEmailAddresses,
                emailCcRecipients = getEmailCcRecipients(request.requestingUserEmailAddress),
                subject = replace(subject, substitutionVariables),
                emailHtmlBody = replace(emailBody, substitutionVariables),
            )
        }
    }

    private fun EmailContentProperties.getEmailCcRecipients(requestingUserEmailAddress: String?) =
        if (sendToRequestingUser && StringUtils.isNotBlank(requestingUserEmailAddress)) {
            setOf(requestingUserEmailAddress!!)
        } else {
            emptySet()
        }
}
