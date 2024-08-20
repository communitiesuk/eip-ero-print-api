package uk.gov.dluhc.printapi.messaging.service

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.email.EmailNotSentException
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.dto.SendCertificateNotDeliveredEmailRequest
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.anEnglishEroContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildProcessPrintResponseMessage

@ExtendWith(MockitoExtension::class)
class CertificateNotDeliveredEmailSenderServiceTest {

    @InjectMocks
    private lateinit var certificateNotDeliveredEmailSenderService: CertificateNotDeliveredEmailSenderService

    @Mock
    private lateinit var electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient
    @Mock
    private lateinit var emailService: EmailService

    @Test
    fun `successfully send email when print response is NOT DELIVERED`() {
        // Given
        val requestId = aValidRequestId()
        val printResponse = buildProcessPrintResponseMessage(
            requestId = requestId,
            status = ProcessPrintResponseMessage.Status.FAILED,
            statusStep = ProcessPrintResponseMessage.StatusStep.NOT_MINUS_DELIVERED,
        )
        val requestingUserEmailAddress = "vc-admin@camden.gov.uk"
        val localAuthorityEmailAddress = "ero.group@@camden.gov.uk"

        val certificate = buildCertificate(
            printRequests = listOf(
                buildPrintRequest(
                    requestId = requestId,
                    userId = requestingUserEmailAddress,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER,
                            eventDateTime = printResponse.timestamp.toInstant().minusSeconds(10)
                        )
                    )
                )
            )
        )
        val expectedGssCode = certificate.gssCode!!

        val eroDto = buildEroDto(englishContactDetails = anEnglishEroContactDetails(emailAddress = localAuthorityEmailAddress))
        given(electoralRegistrationOfficeManagementApiClient.getEro(any())).willReturn(eroDto)

        val expectedSendCertificateNotDeliveredEmailRequest = SendCertificateNotDeliveredEmailRequest(
            sourceReference = certificate.sourceReference!!,
            applicationReference = certificate.applicationReference!!,
            localAuthorityEmailAddresses = setOf(localAuthorityEmailAddress),
            requestingUserEmailAddress = requestingUserEmailAddress
        )

        // When
        certificateNotDeliveredEmailSenderService.send(printResponse, certificate)

        // Then
        verify(electoralRegistrationOfficeManagementApiClient).getEro(expectedGssCode)
        verify(emailService).sendCertificateNotDeliveredEmail(expectedSendCertificateNotDeliveredEmailRequest)
        verifyNoMoreInteractions(electoralRegistrationOfficeManagementApiClient, emailService)
    }

    @Test
    fun `log error message when failed to send email via email service`() {
        // Given
        val requestId = aValidRequestId()
        val printResponse = buildProcessPrintResponseMessage(
            requestId = requestId,
            status = ProcessPrintResponseMessage.Status.FAILED,
            statusStep = ProcessPrintResponseMessage.StatusStep.NOT_MINUS_DELIVERED,
        )
        val certificate = buildCertificate()

        val eroDto = buildEroDto()
        given(electoralRegistrationOfficeManagementApiClient.getEro(any())).willReturn(eroDto)

        given(emailService.sendCertificateNotDeliveredEmail(any()))
            .willThrow(EmailNotSentException("Failed to send email due to AWS error"))
        val expectedLogMessage =
            "failed to send [Certificate Not Delivered] email when processing ProcessPrintResponseMessage for " +
                "certificate [${certificate.id}] with requestId [$requestId]: Failed to send email due to AWS error"

        // When
        certificateNotDeliveredEmailSenderService.send(printResponse, certificate)

        // Then
        assertThat(TestLogAppender.hasLog(expectedLogMessage, Level.ERROR)).isTrue
    }
}
