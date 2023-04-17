package uk.gov.dluhc.printapi.messaging.service

import liquibase.repackaged.org.apache.commons.text.StringSubstitutor.replace
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Service
import uk.gov.dluhc.emailnotifications.EmailClient
import uk.gov.dluhc.printapi.config.EmailContentConfiguration
import uk.gov.dluhc.printapi.config.EmailContentProperties
import uk.gov.dluhc.printapi.dto.SendCertificateFailedToPrintEmailRequest
import uk.gov.dluhc.printapi.dto.SendCertificateNotDeliveredEmailRequest
import uk.gov.dluhc.printapi.dto.SendEmailRequest

@Service
class EmailService(
    private val emailClient: EmailClient,
    private val emailContentConfiguration: EmailContentConfiguration,
) {
    fun sendCertificateNotDeliveredEmail(request: SendCertificateNotDeliveredEmailRequest) {
        val emailConfig = emailContentConfiguration.certificateReturned
        sendEmail(request, emailConfig)
    }

    fun sendCertificateFailedToPrintEmail(request: SendCertificateFailedToPrintEmailRequest) {
        val emailConfig = emailContentConfiguration.certificateFailedToPrint
        sendEmail(request, emailConfig)
    }

    private fun EmailService.sendEmail(
        request: SendEmailRequest,
        emailConfig: EmailContentProperties
    ) {
        val substitutionVariables = with(request) {
            mapOf(
                "sourceReference" to sourceReference,
                "applicationReference" to applicationReference,
                "localAuthorityEmailAddresses" to localAuthorityEmailAddresses,
                "applicationUrl" to "${emailContentConfiguration.vacBaseUrl}/${request.applicationReference}"
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
