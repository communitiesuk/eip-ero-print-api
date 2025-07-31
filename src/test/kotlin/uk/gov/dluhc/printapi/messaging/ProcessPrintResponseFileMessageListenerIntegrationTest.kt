package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.time.Instant
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseFileMessageListenerIntegrationTest : IntegrationTest() {

    @ParameterizedTest
    @NullSource
    @CsvSource("true", "false")
    fun `should fetch remote print response file and send message to source api`(isFromApplicationsApi: Boolean?) {
        // Given
        val filenameToProcess = "status-20220928235441999.json"
        val printResponses = buildPrintResponses()
        val printResponsesAsString = objectMapper.writeValueAsString(printResponses)

        val certificates = printResponses.batchResponses.map {
            buildCertificate(
                status = PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER,
                batchId = it.batchId,
                isFromApplicationsApi = isFromApplicationsApi
            )
        }
        certificateRepository.saveAll(certificates)

        writeContentToRemoteOutBoundDirectory(filenameToProcess, printResponsesAsString)

        val message = ProcessPrintResponseFileMessage(
            directory = PRINT_RESPONSE_DOWNLOAD_PATH,
            fileName = filenameToProcess,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
            certificates.forEach {
                if (isFromApplicationsApi == true) {
                    assertUpdateApplicationStatisticsMessageSent(it.sourceReference!!)
                } else {
                    assertUpdateStatisticsMessageSent(it.sourceReference!!)
                }
            }
        }
    }

    @Test
    fun `should process all SUCCESS print responses and update statuses accordingly`() {
        // Given
        // This file contains 2 print responses with statusStep PRINTED and status SUCCESS,
        // with request ids "request-id-1" and "request-id-2"
        val filenameToProcess = "status-20250715102715200.json.processing"
        writeFileToRemoteOutBoundDirectory(filenameToProcess)

        val certificates = listOf(
            buildCertificate(
                status = PrintRequestStatus.Status.IN_PRODUCTION,
                printRequests = listOf(
                    buildPrintRequest(
                        requestId = "request-id-1",
                        printRequestStatuses = listOf(
                            PrintRequestStatus(
                                status = PrintRequestStatus.Status.IN_PRODUCTION,
                                eventDateTime = Instant.parse("2025-01-01T00:00:00.00Z"),
                            )
                        ),
                    )
                ),
            ),
            buildCertificate(
                status = PrintRequestStatus.Status.IN_PRODUCTION,
                printRequests = listOf(
                    buildPrintRequest(
                        requestId = "request-id-2",
                        printRequestStatuses = listOf(
                            PrintRequestStatus(
                                status = PrintRequestStatus.Status.IN_PRODUCTION,
                                eventDateTime = Instant.parse("2025-01-01T00:00:00.00Z"),
                            )
                        ),
                    ),
                ),
            ),
        )

        certificateRepository.saveAll(certificates)

        val message = ProcessPrintResponseFileMessage(
            directory = PRINT_RESPONSE_DOWNLOAD_PATH,
            fileName = filenameToProcess,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
            certificates.forEach { certificate ->
                val updated = certificateRepository.findById(certificate.id!!).get()
                assertThat(updated.status).isEqualTo(PrintRequestStatus.Status.PRINTED)
                assertThat(
                    updated.printRequests[0].statusHistory
                        .sortedByDescending { it.eventDateTime }
                        .first().status
                )
                    .isEqualTo(PrintRequestStatus.Status.PRINTED)
            }
        }
    }

    @Test
    fun `should process FAILED print responses and update statuses and messages accordingly`() {
        // Given
        // This file contains 2 print responses with statusStep NOT-DELIVERED and status FAILED,
        // with request ids "request-id-1" and "request-id-2"
        val filenameToProcess = "status-20250715103821452.json.processing"
        writeFileToRemoteOutBoundDirectory(filenameToProcess)

        val certificates = listOf(
            buildCertificate(
                status = PrintRequestStatus.Status.PRINTED,
                printRequests = listOf(
                    buildPrintRequest(
                        requestId = "request-id-1",
                        printRequestStatuses = listOf(
                            PrintRequestStatus(
                                status = PrintRequestStatus.Status.PRINTED,
                                eventDateTime = Instant.parse("2025-01-01T00:00:00.00Z"),
                            )
                        ),
                    )
                ),
            ),
            buildCertificate(
                status = PrintRequestStatus.Status.PRINTED,
                printRequests = listOf(
                    buildPrintRequest(
                        requestId = "request-id-2",
                        printRequestStatuses = listOf(
                            PrintRequestStatus(
                                status = PrintRequestStatus.Status.PRINTED,
                                eventDateTime = Instant.parse("2025-01-01T00:00:00.00Z"),
                            )
                        ),
                    ),
                ),
            ),
        )

        certificateRepository.saveAll(certificates)

        val message = ProcessPrintResponseFileMessage(
            directory = PRINT_RESPONSE_DOWNLOAD_PATH,
            fileName = filenameToProcess,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
            certificates.forEach { certificate ->
                val updated = certificateRepository.findById(certificate.id!!).get()
                assertThat(updated.status).isEqualTo(PrintRequestStatus.Status.NOT_DELIVERED)
                assertThat(
                    updated.printRequests.first().statusHistory
                        .sortedByDescending { it.eventDateTime }
                        .first().status
                )
                    .isEqualTo(PrintRequestStatus.Status.NOT_DELIVERED)
                assertThat(
                    updated.printRequests.first().statusHistory
                        .sortedByDescending { it.eventDateTime }
                        .first().message
                )
                    .isNotNull()
            }
        }
    }
}
