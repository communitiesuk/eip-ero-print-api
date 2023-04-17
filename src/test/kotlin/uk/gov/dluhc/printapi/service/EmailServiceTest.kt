package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.emailnotifications.EmailClient
import uk.gov.dluhc.printapi.config.EmailContentConfiguration
import uk.gov.dluhc.printapi.config.EmailContentProperties
import uk.gov.dluhc.printapi.dto.SendCertificateFailedToPrintEmailRequest
import uk.gov.dluhc.printapi.dto.SendCertificateNotDeliveredEmailRequest
import uk.gov.dluhc.printapi.messaging.service.EmailService
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference

@ExtendWith(MockitoExtension::class)
internal class EmailServiceTest {
    @InjectMocks
    private lateinit var emailService: EmailService

    @Mock
    private lateinit var emailClient: EmailClient

    @Mock
    private lateinit var emailContentConfiguration: EmailContentConfiguration

    @Nested
    inner class SendCertificateNotDeliveredEmail {

        @Test
        fun `should successfully send a Certificate Returned email given sendToRequestingUser is disabled`() {
            // Given
            val sourceReference = aValidSourceReference()
            val applicationReference = aValidApplicationReference()
            val localAuthorityEmailAddresses = setOf("electoral.services@camden.gov.uk")
            val requestingUserEmailAddress = "joe.bloggs@camden.gov.uk"
            val sendToRequestingUser = false

            val expectedToRecipients = setOf("electoral.services@camden.gov.uk")
            val expectedCcRecipients = emptySet<String>()
            val expectedSubject = "Electoral Registration Office Portal - certificate returned - application $applicationReference"
            val expectedEmailBody = """
                <html>
                  <body>
                    <p>The print provider has notified us that a postal certificate has been returned.</p>
                    <p>You can access the application here:</p>
                    <p><a href="https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference">
                                https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference</a>
                    </p>
                    <br>
                    <p>This is an automated message from the ERO Portal.  You will not be able to reply to this email.</p>
                  </body>
                </html>
            """.lines().joinToString(separator = "\\s*") { it.trim() }

            given(emailContentConfiguration.vacBaseUrl).willReturn("https://ero.dev.erop.ierds.uk/voter-authority-certificate")
            val emailContentProperties = buildEmailContentProperties(
                sendToRequestingUser = sendToRequestingUser,
                emailBodyTemplate = "email-templates/prod/certificate-returned.html"
            )
            given(emailContentConfiguration.certificateReturned).willReturn(emailContentProperties)

            // When
            emailService.sendCertificateNotDeliveredEmail(
                SendCertificateNotDeliveredEmailRequest(
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    localAuthorityEmailAddresses = localAuthorityEmailAddresses,
                    requestingUserEmailAddress = requestingUserEmailAddress
                )
            )

            // Then
            argumentCaptor<String>().apply {
                verify(emailClient).send(
                    eq(expectedToRecipients),
                    eq(expectedCcRecipients),
                    eq(expectedSubject),
                    capture()
                )
                assertThat(firstValue).matches(expectedEmailBody)
            }
            verify(emailContentConfiguration).vacBaseUrl
            verify(emailContentConfiguration).certificateReturned
            verifyNoMoreInteractions(emailClient, emailContentConfiguration)
        }

        @Test
        fun `should successfully send a Certificate Returned email given sendToRequestingUser is enabled`() {
            // Given
            val sourceReference = aValidSourceReference()
            val applicationReference = aValidApplicationReference()
            val localAuthorityEmailAddresses = setOf("electoral.services@camden.gov.uk")
            val requestingUserEmailAddress = "joe.bloggs@camden.gov.uk"
            val sendToRequestingUser = true

            val expectedToRecipients = setOf("electoral.services@camden.gov.uk")
            val expectedCcRecipients = setOf("joe.bloggs@camden.gov.uk")
            val expectedSubject = "Electoral Registration Office Portal - certificate returned - application $applicationReference"
            val expectedEmailBody = """
                <html>
                  <body>
                    <h1>NOTE: This is a test email</h1>
                    <br>
                    <p>The print provider has notified us that a postal certificate has been returned.</p>
                    <p>You can access the application here:</p>
                    <p><a href="https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference">
                                https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference</a>
                    </p>
                    <br>
                    <p>Local Authority email addresses: \[electoral.services@camden.gov.uk]</p>
                    <p>This is an automated message from the ERO Portal.  You will not be able to reply to this email.</p>
                  </body>
                </html>
            """.lines().joinToString(separator = "\\s*") { it.trim() }

            given(emailContentConfiguration.vacBaseUrl).willReturn("https://ero.dev.erop.ierds.uk/voter-authority-certificate")
            val emailContentProperties = buildEmailContentProperties(
                sendToRequestingUser = sendToRequestingUser,
                emailBodyTemplate = "email-templates/non-prod/certificate-returned.html"
            )
            given(emailContentConfiguration.certificateReturned).willReturn(emailContentProperties)

            // When
            emailService.sendCertificateNotDeliveredEmail(
                SendCertificateNotDeliveredEmailRequest(
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    localAuthorityEmailAddresses = localAuthorityEmailAddresses,
                    requestingUserEmailAddress = requestingUserEmailAddress
                )
            )

            // Then
            argumentCaptor<String>().apply {
                verify(emailClient).send(
                    eq(expectedToRecipients),
                    eq(expectedCcRecipients),
                    eq(expectedSubject),
                    capture()
                )
                assertThat(firstValue).matches(expectedEmailBody)
            }
            verify(emailContentConfiguration).vacBaseUrl
            verify(emailContentConfiguration).certificateReturned
            verifyNoMoreInteractions(emailClient, emailContentConfiguration)
        }

        private fun buildEmailContentProperties(
            sendToRequestingUser: Boolean,
            emailBodyTemplate: String,
        ) = EmailContentProperties(
            subject = "Electoral Registration Office Portal - certificate returned - application \${applicationReference}",
            emailBodyTemplate = emailBodyTemplate,
            sendToRequestingUser = sendToRequestingUser,
        )
    }

    @Nested
    inner class SendCertificateFailedToPrintEmail {

        @Test
        fun `should successfully send a Certificate Failed To Print email given sendToRequestingUser is disabled`() {
            // Given
            val sourceReference = aValidSourceReference()
            val applicationReference = aValidApplicationReference()
            val localAuthorityEmailAddresses = setOf("electoral.services@camden.gov.uk")
            val requestingUserEmailAddress = "joe.bloggs@camden.gov.uk"
            val sendToRequestingUser = false

            val expectedToRecipients = setOf("electoral.services@camden.gov.uk")
            val expectedCcRecipients = emptySet<String>()
            val expectedSubject = "Electoral Registration Office Portal - printing failed - application $applicationReference"
            val expectedEmailBody = """
                <html>
                  <body>
                    <p>Printing has failed for the application $applicationReference</p>
                    <p>You can access the application here:</p>
                    <p><a href="https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference">
                                https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference</a>
                    </p>
                    <br>
                    <p>This is an automated message from the ERO Portal.  You will not be able to reply to this email.</p>
                  </body>
                </html>
            """.lines().joinToString(separator = "\\s*") { it.trim() }

            given(emailContentConfiguration.vacBaseUrl).willReturn("https://ero.dev.erop.ierds.uk/voter-authority-certificate")
            val emailContentProperties = buildEmailContentProperties(
                sendToRequestingUser = sendToRequestingUser,
                emailBodyTemplate = "email-templates/prod/certificate-failed-to-print.html"
            )
            given(emailContentConfiguration.certificateFailedToPrint).willReturn(emailContentProperties)

            // When
            emailService.sendCertificateFailedToPrintEmail(
                SendCertificateFailedToPrintEmailRequest(
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    localAuthorityEmailAddresses = localAuthorityEmailAddresses,
                    requestingUserEmailAddress = requestingUserEmailAddress
                )
            )

            // Then
            argumentCaptor<String>().apply {
                verify(emailClient).send(
                    eq(expectedToRecipients),
                    eq(expectedCcRecipients),
                    eq(expectedSubject),
                    capture()
                )
                assertThat(firstValue).matches(expectedEmailBody)
            }
            verify(emailContentConfiguration).vacBaseUrl
            verify(emailContentConfiguration).certificateFailedToPrint
            verifyNoMoreInteractions(emailClient, emailContentConfiguration)
        }

        @Test
        fun `should successfully send a Certificate Failed To Print email given sendToRequestingUser is enabled`() {
            // Given
            val sourceReference = aValidSourceReference()
            val applicationReference = aValidApplicationReference()
            val localAuthorityEmailAddresses = setOf("electoral.services@camden.gov.uk")
            val requestingUserEmailAddress = "joe.bloggs@camden.gov.uk"
            val sendToRequestingUser = true

            val expectedToRecipients = setOf("electoral.services@camden.gov.uk")
            val expectedCcRecipients = setOf("joe.bloggs@camden.gov.uk")
            val expectedSubject = "Electoral Registration Office Portal - printing failed - application $applicationReference"
            val expectedEmailBody = """
                <html>
                  <body>
                    <h1>NOTE: This is a test email</h1>
                    <br>
                    <p>Printing has failed for the application $applicationReference</p>
                    <p>You can access the application here:</p>
                    <p><a href="https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference">
                                https://ero.dev.erop.ierds.uk/voter-authority-certificate/$applicationReference</a>
                    </p>
                    <br>
                    <p>Local Authority email addresses: \[electoral.services@camden.gov.uk]</p>
                    <p>This is an automated message from the ERO Portal.  You will not be able to reply to this email.</p>
                  </body>
                </html>
            """.lines().joinToString(separator = "\\s*") { it.trim() }

            given(emailContentConfiguration.vacBaseUrl).willReturn("https://ero.dev.erop.ierds.uk/voter-authority-certificate")
            val emailContentProperties = buildEmailContentProperties(
                sendToRequestingUser = sendToRequestingUser,
                emailBodyTemplate = "email-templates/non-prod/certificate-failed-to-print.html"
            )
            given(emailContentConfiguration.certificateFailedToPrint).willReturn(emailContentProperties)

            // When
            emailService.sendCertificateFailedToPrintEmail(
                SendCertificateFailedToPrintEmailRequest(
                    sourceReference = sourceReference,
                    applicationReference = applicationReference,
                    localAuthorityEmailAddresses = localAuthorityEmailAddresses,
                    requestingUserEmailAddress = requestingUserEmailAddress
                )
            )

            // Then
            argumentCaptor<String>().apply {
                verify(emailClient).send(
                    eq(expectedToRecipients),
                    eq(expectedCcRecipients),
                    eq(expectedSubject),
                    capture()
                )
                assertThat(firstValue).matches(expectedEmailBody)
            }
            verify(emailContentConfiguration).vacBaseUrl
            verify(emailContentConfiguration).certificateFailedToPrint
            verifyNoMoreInteractions(emailClient, emailContentConfiguration)
        }

        private fun buildEmailContentProperties(
            sendToRequestingUser: Boolean,
            emailBodyTemplate: String,
        ) = EmailContentProperties(
            subject = "Electoral Registration Office Portal - printing failed - application \${applicationReference}",
            emailBodyTemplate = emailBodyTemplate,
            sendToRequestingUser = sendToRequestingUser,
        )
    }
}
