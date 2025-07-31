package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.testsupport.emails.buildLocalstackEmailMessage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSuggestedExpiryDate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildProcessPrintResponseMessage
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import java.time.Instant
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.JULY
import java.time.Month.JUNE
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit.SECONDS
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseMessageListenerIntegrationTest : IntegrationTest() {

    @ParameterizedTest
    @NullSource
    @CsvSource("true", "false")
    fun `should process print response message`(isFromApplicationsApi: Boolean?) {
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
            ),
            isFromApplicationsApi = isFromApplicationsApi
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
            if (isFromApplicationsApi == true) {
                assertUpdateApplicationStatisticsMessageSent(certificate.sourceReference!!)
            } else {
                assertUpdateStatisticsMessageSent(certificate.sourceReference!!)
            }
        }
    }

    @Test
    fun `should process print response message for successful print step for a certificate with an existing source application`() {
        // Given
        val requestId = aValidRequestId()
        val batchId = aValidBatchId()
        val certificate = buildCertificate(
            issueDate = null,
            suggestedExpiryDate = null,
            status = Status.ASSIGNED_TO_BATCH,
            hasSourceApplicationBeenRemoved = false,
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

        val issueDate = aValidIssueDate()
        val suggestedExpiryDate = aValidSuggestedExpiryDate()

        val message = buildProcessPrintResponseMessage(
            requestId = requestId,
            status = ProcessPrintResponseMessage.Status.SUCCESS,
            statusStep = ProcessPrintResponseMessage.StatusStep.PRINTED,
            issueDate = issueDate,
            suggestedExpiryDate = suggestedExpiryDate,
        )

        // When
        processPrintResponseMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val saved = certificateRepository.getByPrintRequestsRequestId(requestId)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(Status.PRINTED)
            assertThat(saved.issueDate).isNotNull
            assertThat(saved.suggestedExpiryDate).isNotNull
            assertThat(saved.issueDate).isEqualTo(issueDate)
            assertThat(saved.suggestedExpiryDate).isEqualTo(suggestedExpiryDate)
            assertUpdateStatisticsMessageSent(certificate.sourceReference!!)
        }
    }

    @Test
    fun `should process print response message for successful print step for a certificate with a removed source application`() {
        // Given
        val requestId = aValidRequestId()
        val batchId = aValidBatchId()
        val certificate = buildCertificate(
            issueDate = null,
            suggestedExpiryDate = null,
            status = Status.ASSIGNED_TO_BATCH,
            hasSourceApplicationBeenRemoved = true,
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

        // Calculating the number of working days is non-trivial in a test,
        // so we're using a set date for the test instead of today
        val issueDate = LocalDate.of(2025, APRIL, 20)
        val suggestedExpiryDate = LocalDate.of(2035, APRIL, 20)
        // 28 working days after the issue date (6 weekends, 3 bank holidays)
        val initialRetentionRemovalDate = LocalDate.of(2025, JUNE, 2)
        val finalRetentionRemovalDate = LocalDate.of(2034, JULY, 1)

        val message = buildProcessPrintResponseMessage(
            requestId = requestId,
            status = ProcessPrintResponseMessage.Status.SUCCESS,
            statusStep = ProcessPrintResponseMessage.StatusStep.PRINTED,
            issueDate = issueDate,
            suggestedExpiryDate = suggestedExpiryDate,
        )

        // When
        processPrintResponseMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val saved = certificateRepository.getByPrintRequestsRequestId(requestId)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(Status.PRINTED)
            assertThat(saved.issueDate).isNotNull
            assertThat(saved.suggestedExpiryDate).isNotNull
            assertThat(saved.issueDate).isEqualTo(issueDate)
            assertThat(saved.suggestedExpiryDate).isEqualTo(suggestedExpiryDate)
            assertThat(saved.initialRetentionRemovalDate).isNotNull
            assertThat(saved.finalRetentionRemovalDate).isNotNull
            assertThat(saved.initialRetentionRemovalDate).isEqualTo(initialRetentionRemovalDate)
            assertThat(saved.finalRetentionRemovalDate).isEqualTo(finalRetentionRemovalDate)

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
                    contactDetailsEnglish = buildContactDetails(emailAddressVac = "a-user@valtech.com")
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
            statusStep = ProcessPrintResponseMessage.StatusStep.PROCESSED,
        )
        val expectedStatus = Status.PRINT_PROVIDER_VALIDATION_FAILED

        val expectedGssCode = certificate.gssCode!!
        val ero = buildElectoralRegistrationOfficeResponse(
            localAuthorities = listOf(
                buildLocalAuthorityResponse(
                    gssCode = expectedGssCode,
                    contactDetailsEnglish = buildContactDetails(emailAddressVac = "a-user@valtech.com")
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
