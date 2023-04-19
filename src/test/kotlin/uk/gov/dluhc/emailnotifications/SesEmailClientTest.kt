package uk.gov.dluhc.emailnotifications

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import software.amazon.awssdk.services.ses.SesClient
import software.amazon.awssdk.services.ses.model.AccountSendingPausedException
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import uk.gov.dluhc.emailnotifications.testsupport.buildSendEmailResponse

@ExtendWith(MockitoExtension::class)
class SesEmailClientTest {
    companion object {
        private const val APPLICATION_REF = "VD7J031K02"
        private const val APPLICATION_ID = "e70941ae9e4cb95057257d50"
        private const val EMAIL_SUBJECT = "Electoral Registration Office Portal - certificate returned - application - $APPLICATION_REF"
        private const val EMAIL_BODY = "<html><body><h1><a href=\"$APPLICATION_ID\" /></h1><body><html>"

        private const val USER1_ON_ALLOW_LIST = "vc-admin-1@valtech.com"
        private const val USER2_ON_ALLOW_LIST = "vc-admin-2@softwire.com"
        private const val USER1_NOT_ON_ALLOW_LIST = "unknown.user1@not-on-allow-list.com"
        private const val USER2_NOT_ON_ALLOW_LIST = "unknown.user2@not-on-allow-list.com"
        private val ALLOW_LISTED_DOMAINS = setOf("levellingup.gov.uk", "valtech.com", "softwire.com")

        private const val SENDERS_EMAIL_ADDRESS = "noreply_dev_erouser@erop.ierds.uk"
        private const val SES_MESSAGE_ID = "jwndkkpobyredayr-boonxyse-rxdm-ltcw-tasw-shovbwefolvk-fnmzmw"
    }

    private lateinit var sesEmailClient: SesEmailClient

    @Mock
    private lateinit var sesClient: SesClient

    @Test
    fun `successfully send an email to recipients from allow listed domains`() {
        // Given
        val recipientsOnAllowList = setOf(USER1_ON_ALLOW_LIST)
        val ccRecipientsOnAllowList = setOf(USER2_ON_ALLOW_LIST)
        val allowListEnabled = true
        val allowListedDomains = ALLOW_LISTED_DOMAINS

        val expectedRecipientsOfEmail = setOf(USER1_ON_ALLOW_LIST, USER2_ON_ALLOW_LIST)
        val expectedEmailRequest = buildSendEmailRequest(
            sender = SENDERS_EMAIL_ADDRESS,
            emailToAddresses = setOf(USER1_ON_ALLOW_LIST),
            emailCcAddresses = setOf(USER2_ON_ALLOW_LIST),
            subject = EMAIL_SUBJECT,
            emailHtmlBody = EMAIL_BODY,
        )

        val sendEmailResponse = buildSendEmailResponse(SES_MESSAGE_ID)
        given(sesClient.sendEmail(any<SendEmailRequest>())).willReturn(sendEmailResponse)

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val actualRecipients = sesEmailClient.send(recipientsOnAllowList, ccRecipientsOnAllowList, EMAIL_SUBJECT, EMAIL_BODY)

        // Then
        assertThat(actualRecipients).containsExactlyElementsOf(expectedRecipientsOfEmail)
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `successfully send emails when the allow list domains are disabled`() {
        // Given
        val toRecipientsOnAndOffAllowList = setOf(USER1_ON_ALLOW_LIST, USER1_NOT_ON_ALLOW_LIST)
        val ccRecipientsOnAndOffAllowList = setOf(USER2_ON_ALLOW_LIST, USER2_NOT_ON_ALLOW_LIST)
        val allowListEnabled = false
        val allowListedDomains = emptySet<String>()

        val expectedRecipientsOfEmail = setOf(USER1_ON_ALLOW_LIST, USER1_NOT_ON_ALLOW_LIST, USER2_ON_ALLOW_LIST, USER2_NOT_ON_ALLOW_LIST)
        val expectedEmailRequest = buildSendEmailRequest(
            sender = SENDERS_EMAIL_ADDRESS,
            emailToAddresses = setOf(USER1_ON_ALLOW_LIST, USER1_NOT_ON_ALLOW_LIST),
            emailCcAddresses = setOf(USER2_ON_ALLOW_LIST, USER2_NOT_ON_ALLOW_LIST),
            subject = EMAIL_SUBJECT,
            emailHtmlBody = EMAIL_BODY,
        )

        val sendEmailResponse = buildSendEmailResponse(SES_MESSAGE_ID)
        given(sesClient.sendEmail(any<SendEmailRequest>())).willReturn(sendEmailResponse)

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val actualRecipients = sesEmailClient.send(toRecipientsOnAndOffAllowList, ccRecipientsOnAndOffAllowList, EMAIL_SUBJECT, EMAIL_BODY)

        // Then
        assertThat(actualRecipients).containsExactlyElementsOf(expectedRecipientsOfEmail)
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `successfully send an email to all recipients when domain allow list is empty`() {
        // Given
        val toRecipients = setOf("user1@hotmail.com", "user2@gmail.com")
        val ccRecipientsOnAllowList = setOf("user1@protonmail.com")
        val allowListEnabled = true
        val allowListedDomains = emptySet<String>()

        val expectedRecipientsOfEmail = setOf("user1@hotmail.com", "user2@gmail.com", "user1@protonmail.com")
        val expectedEmailRequest = buildSendEmailRequest(
            sender = SENDERS_EMAIL_ADDRESS,
            emailToAddresses = setOf("user1@hotmail.com", "user2@gmail.com"),
            emailCcAddresses = setOf("user1@protonmail.com"),
            subject = EMAIL_SUBJECT,
            emailHtmlBody = EMAIL_BODY,
        )

        val sendEmailResponse = buildSendEmailResponse(SES_MESSAGE_ID)
        given(sesClient.sendEmail(any<SendEmailRequest>())).willReturn(sendEmailResponse)

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val actualRecipients = sesEmailClient.send(toRecipients, ccRecipientsOnAllowList, EMAIL_SUBJECT, EMAIL_BODY)

        // Then
        assertThat(actualRecipients).containsExactlyElementsOf(expectedRecipientsOfEmail)
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `should not send an email when none of the recipients are from allow listed domains`() {
        // Given
        val toRecipients = setOf(USER1_NOT_ON_ALLOW_LIST)
        val ccRecipients = setOf(USER2_NOT_ON_ALLOW_LIST)
        val allowListEnabled = true
        val allowListedDomains = ALLOW_LISTED_DOMAINS

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val actualRecipients = sesEmailClient.send(toRecipients, ccRecipients, EMAIL_SUBJECT, EMAIL_BODY)

        // Then
        assertThat(actualRecipients).isEmpty()
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `successfully send an email only to recipients from the allow listed domain`() {
        // Given
        val toRecipientsOnAndOffAllowList = setOf(USER1_NOT_ON_ALLOW_LIST, USER1_ON_ALLOW_LIST)
        val ccRecipientsOnAndOffAllowList = setOf(USER2_NOT_ON_ALLOW_LIST, USER2_ON_ALLOW_LIST)
        val allowListEnabled = true
        val allowListedDomains = ALLOW_LISTED_DOMAINS

        val expectedRecipientsOfEmail = setOf(USER1_ON_ALLOW_LIST, USER2_ON_ALLOW_LIST)
        val expectedEmailRequest = buildSendEmailRequest(
            sender = SENDERS_EMAIL_ADDRESS,
            emailToAddresses = setOf(USER1_ON_ALLOW_LIST),
            emailCcAddresses = setOf(USER2_ON_ALLOW_LIST),
            subject = EMAIL_SUBJECT,
            emailHtmlBody = EMAIL_BODY,
        )

        val sendEmailResponse = buildSendEmailResponse(SES_MESSAGE_ID)
        given(sesClient.sendEmail(any<SendEmailRequest>())).willReturn(sendEmailResponse)

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val actualRecipients = sesEmailClient.send(toRecipientsOnAndOffAllowList, ccRecipientsOnAndOffAllowList, EMAIL_SUBJECT, EMAIL_BODY)

        // Then
        assertThat(actualRecipients).containsExactlyElementsOf(expectedRecipientsOfEmail)
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `should throw an exception when the email fails to send due to an aws exception`() {
        // Given
        val toRecipientOnAllowList = setOf(USER1_ON_ALLOW_LIST)
        val ccRecipientsEmptySet = emptySet<String>()
        val allowListEnabled = true
        val allowListedDomains = ALLOW_LISTED_DOMAINS

        val expectedEmailRequest = buildSendEmailRequest(
            sender = SENDERS_EMAIL_ADDRESS,
            emailToAddresses = setOf(USER1_ON_ALLOW_LIST),
            emailCcAddresses = emptySet(),
            subject = EMAIL_SUBJECT,
            emailHtmlBody = EMAIL_BODY,
        )

        val awsException = AccountSendingPausedException.builder().message("please deposit more money").build()
        given(sesClient.sendEmail(any<SendEmailRequest>())).willThrow(awsException)

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val ex = Assertions.catchThrowableOfType(
            { sesEmailClient.send(toRecipientOnAllowList, ccRecipientsEmptySet, EMAIL_SUBJECT, EMAIL_BODY) },
            EmailNotSentException::class.java
        )

        // Then
        assertThat(ex).isNotNull
        assertThat(ex).hasMessage("Failed to send email to [$toRecipientOnAllowList], cc[[]] with subject[$EMAIL_SUBJECT]")
        verify(sesClient).sendEmail(expectedEmailRequest)
        verifyNoMoreInteractions(sesClient)
    }

    @Test
    fun `should throw an exception when no to-recipients nor cc-recipients are specified`() {
        // Given
        val toRecipientsEmptySet = emptySet<String>()
        val ccRecipientsEmptySet = emptySet<String>()
        val allowListEnabled = true
        val allowListedDomains = ALLOW_LISTED_DOMAINS

        // When
        sesEmailClient = buildSesEmailClientWithAllowList(allowListEnabled, allowListedDomains)
        val ex = Assertions.catchThrowableOfType(
            { sesEmailClient.send(toRecipientsEmptySet, ccRecipientsEmptySet, EMAIL_SUBJECT, EMAIL_BODY) },
            EmailNotSentException::class.java
        )

        // Then
        assertThat(ex).isNotNull
        assertThat(ex).hasMessage("Failed to send email due to recipientsToEmail and recipientsToCc being empty")
        verifyNoMoreInteractions(sesClient)
    }

    private fun buildSesEmailClientWithAllowList(
        allowListEnabled: Boolean,
        allowListedDomains: Set<String>,
    ) = SesEmailClient(sesClient, SENDERS_EMAIL_ADDRESS, allowListEnabled, allowListedDomains)
}
