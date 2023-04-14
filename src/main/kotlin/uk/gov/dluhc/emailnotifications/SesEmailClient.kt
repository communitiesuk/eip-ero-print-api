package uk.gov.dluhc.emailnotifications

import mu.KotlinLogging
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SesException

private val logger = KotlinLogging.logger {}

class SesEmailClient(
    private val sesClient: SesClient,
    private val sender: String,
    private val allowListEnabled: Boolean,
    private val allowListDomains: Set<String>,
) : EmailClient {

    override fun send(emailToRecipients: Set<String>, emailCcRecipients: Set<String>, subject: String, emailHtmlBody: String): Set<String> {
        if (emailToRecipients.isEmpty() && emailCcRecipients.isEmpty()) {
            throw EmailNotSentException("Failed to send email due to recipientsToEmail and recipientsToCc being empty")
        }

        val filteredEmailToAddresses = filterRecipientsIfAllowListConfigured(emailToRecipients, subject)
        val filteredEmailCcAddresses = filterRecipientsIfAllowListConfigured(emailCcRecipients, subject)
        if (filteredEmailToAddresses.isEmpty() && filteredEmailCcAddresses.isEmpty()) {
            return emptySet()
        }

        val emailRequest: SendEmailRequest = buildSendEmailRequest(
            sender = sender,
            emailToAddresses = filteredEmailToAddresses,
            emailCcAddresses = filteredEmailCcAddresses,
            subject = subject,
            emailHtmlBody = emailHtmlBody,
        )

        try {
            logger.debug { "sending an email to [$filteredEmailToAddresses], cc[$filteredEmailCcAddresses] with subject[$subject]" }
            val messageId = sesClient.sendEmail(emailRequest).messageId()
            logger.debug { "email sent with messageId[$messageId]" }
        } catch (e: SesException) {
            logger.error("failed to send email to [$filteredEmailToAddresses], cc[$filteredEmailCcAddresses] with subject[$subject]", e)
            throw EmailNotSentException(filteredEmailToAddresses, filteredEmailCcAddresses, subject, e)
        }
        return filteredEmailToAddresses + filteredEmailCcAddresses
    }

    private fun filterRecipientsIfAllowListConfigured(recipients: Set<String>, subject: String): Set<String> =
        if (allowListEnabled && allowListDomains.isNotEmpty()) {
            filterRecipientsOnAllowList(recipients, subject)
        } else {
            recipients
        }

    private fun filterRecipientsOnAllowList(recipients: Set<String>, subject: String): Set<String> {
        val (allowListedEmailAddresses, nonAllowListedEmailAddresses) =
            recipients.partition { it.domainOfEmailAddress() in allowListDomains }

        nonAllowListedEmailAddresses.forEach {
            logger.warn { "email not sent due to allow list constraint:  [$it], subject[$subject]" }
        }

        return allowListedEmailAddresses.toSet()
    }

    private fun String.domainOfEmailAddress() = substringAfterLast("@")
}
