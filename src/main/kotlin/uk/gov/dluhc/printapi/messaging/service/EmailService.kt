package uk.gov.dluhc.printapi.messaging.service

import liquibase.repackaged.org.apache.commons.text.StringSubstitutor.replace
import org.springframework.stereotype.Service
import uk.gov.dluhc.emailnotifications.EmailClient
import uk.gov.dluhc.printapi.config.EmailContentConfiguration
import uk.gov.dluhc.printapi.config.EmailContentProperties
import uk.gov.dluhc.printapi.dto.SendCertificateNotDeliveredEmailRequest

@Service
class EmailService(
    private val emailClient: EmailClient,
    private val emailContentConfiguration: EmailContentConfiguration,
) {
    fun sendCertificateNotDeliveredEmail(request: SendCertificateNotDeliveredEmailRequest) {
        val substitutionVariables = with(request) {
            mapOf(
                "sourceReference" to sourceReference,
                "applicationReference" to applicationReference,
                "localAuthorityEmailAddresses" to localAuthorityEmailAddresses,
                "applicationUrl" to "${emailContentConfiguration.vacBaseUrl}/${request.applicationReference}"
            )
        }

        with(emailContentConfiguration.certificateReturned) {
            emailClient.send(
                emailToRecipients = request.localAuthorityEmailAddresses,
                emailCcRecipients = getEmailCcRecipients(request.requestingUserEmailAddress),
                subject = replace(subject, substitutionVariables),
                emailHtmlBody = replace(emailBody, substitutionVariables),
            )
        }
    }

    private fun EmailContentProperties.getEmailCcRecipients(requestingUserEmailAddress: String?) =
        if (sendToRequestingUser && requestingUserEmailAddress?.isNotBlank() == true) {
            setOf(requestingUserEmailAddress)
        } else {
            emptySet()
        }
}
