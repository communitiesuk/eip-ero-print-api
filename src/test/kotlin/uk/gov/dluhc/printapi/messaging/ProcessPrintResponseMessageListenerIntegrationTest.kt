package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.testsupport.emails.buildLocalstackEmailMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildProcessPrintResponseMessage
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.SECONDS
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseMessageListenerIntegrationTest : IntegrationTest() {
    @Test
    fun `should process print response message`() {
        // Given
        val requestId = aValidRequestId()
        val batchId = aValidBatchId()
        val certificate = buildCertificate(
            status = Status.ASSIGNED_TO_BATCH,
            printRequests = mutableListOf(
                buildPrintRequest(
                    batchId = batchId,
                    requestId = requestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = Status.ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                )
            )
        )
        certificateRepository.save(certificate)

        val printResponse = buildPrintResponse(
            requestId = requestId,
            status = PrintResponse.Status.SUCCESS,
            statusStep = PrintResponse.StatusStep.IN_PRODUCTION
        )

        val message = ProcessPrintResponseMessage(
            requestId = requestId,
            timestamp = printResponse.timestamp,
            status = ProcessPrintResponseMessage.Status.SUCCESS,
            statusStep = ProcessPrintResponseMessage.StatusStep.IN_MINUS_PRODUCTION,
            message = printResponse.message,
        )

        // When
        processPrintResponseMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val saved = certificateRepository.getByPrintRequestsRequestId(printResponse.requestId)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(Status.IN_PRODUCTION)
            assertUpdateStatisticsMessageSent(certificate.sourceReference!!)
        }
    }

    @Test
    fun `should process print response message and send Certificate Returned email`() {
        // Given
        val requestId = aValidRequestId()
        val batchId = aValidBatchId()
        val certificate = buildCertificate(
            status = Status.ASSIGNED_TO_BATCH,
            printRequests = mutableListOf(
                buildPrintRequest(
                    batchId = batchId,
                    requestId = requestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = Status.ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                )
            )
        )
        certificateRepository.save(certificate)

        val message = buildProcessPrintResponseMessage(
            requestId = requestId,
            status = ProcessPrintResponseMessage.Status.FAILED,
            statusStep = ProcessPrintResponseMessage.StatusStep.NOT_MINUS_DELIVERED,
        )
        val expectedStatus = Status.NOT_DELIVERED

        val expectedGssCode = certificate.gssCode!!
        val ero = buildElectoralRegistrationOfficeResponse(
            localAuthorities = listOf(
                buildLocalAuthorityResponse(
                    gssCode = expectedGssCode,
                    contactDetailsEnglish = buildContactDetails(emailAddress = "a-user@valtech.com")
                ),
            )
        )
        wireMockService.stubEroManagementGetEroByGssCode(ero, expectedGssCode)

        val expectedEmailMessage = buildLocalstackEmailMessage(
            timestamp = OffsetDateTime.now(UTC).toLocalDateTime().truncatedTo(SECONDS),
            emailSender = "noreply_erouser@erop.ierds.uk",
            toAddresses = setOf("a-user@valtech.com"),
            subject = "Electoral Registration Office Portal - certificate returned - application ${certificate.applicationReference}",
            // just asserting that the application reference is contained within the email body
            htmlBody = "((.|\\s)*)${certificate.applicationReference}((.|\\s)*)"
        )

        // When
        processPrintResponseMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val saved = certificateRepository.getByPrintRequestsRequestId(requestId)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(expectedStatus)
            wireMockService.verifyEroManagementGetEro(expectedGssCode)
            assertEmailSent(expectedEmailMessage)
        }
    }

    @Test
    fun `should process print response message and send Certificate Failed To Print email`() {
        // Given
        val requestId = aValidRequestId()
        val batchId = aValidBatchId()
        val certificate = buildCertificate(
            status = Status.ASSIGNED_TO_BATCH,
            printRequests = mutableListOf(
                buildPrintRequest(
                    batchId = batchId,
                    requestId = requestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = Status.ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                )
            )
        )
        certificateRepository.save(certificate)

        val message = buildProcessPrintResponseMessage(
            requestId = requestId,
            status = ProcessPrintResponseMessage.Status.FAILED,
            statusStep = ProcessPrintResponseMessage.StatusStep.IN_MINUS_PRODUCTION,
        )
        val expectedStatus = Status.PRINT_PROVIDER_PRODUCTION_FAILED

        val expectedGssCode = certificate.gssCode!!
        val ero = buildElectoralRegistrationOfficeResponse(
            localAuthorities = listOf(
                buildLocalAuthorityResponse(
                    gssCode = expectedGssCode,
                    contactDetailsEnglish = buildContactDetails(emailAddress = "a-user@valtech.com")
                ),
            )
        )
        wireMockService.stubEroManagementGetEroByGssCode(ero, expectedGssCode)

        val expectedEmailMessage = buildLocalstackEmailMessage(
            timestamp = OffsetDateTime.now(UTC).toLocalDateTime().truncatedTo(SECONDS),
            emailSender = "noreply_erouser@erop.ierds.uk",
            toAddresses = setOf("a-user@valtech.com"),
            subject = "Electoral Registration Office Portal - printing failed - application ${certificate.applicationReference}",
            // just asserting that the application reference is contained within the email body
            htmlBody = "((.|\\s)*)${certificate.applicationReference}((.|\\s)*)"
        )

        // When
        processPrintResponseMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val saved = certificateRepository.getByPrintRequestsRequestId(requestId)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(expectedStatus)
            wireMockService.verifyEroManagementGetEro(expectedGssCode)
            assertEmailSent(expectedEmailMessage)
        }
    }
}
