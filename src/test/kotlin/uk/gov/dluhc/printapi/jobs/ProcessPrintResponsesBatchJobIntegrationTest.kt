package uk.gov.dluhc.printapi.jobs

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.printprovider.models.BatchResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.testsupport.deepCopy
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponsesBatchJobIntegrationTest : IntegrationTest() {
    @Test
    fun `should process outbound directory for print responses`() {
        // Given
        val validFile1 = "status-20221101171156056.json"
        val validFile2 = "status-20221101171156057.json"
        val validFile3 = "status-20220928235441000.json"
        val unknownFileTypeFileName = "status-unknown-file.json"

        writeFileToRemoteOutBoundDirectory(validFile1)
        writeFileToRemoteOutBoundDirectory(validFile2)
        writeContentToRemoteOutBoundDirectory(validFile3, objectMapper.writeValueAsString(buildPrintResponses()))
        writeContentToRemoteOutBoundDirectory(unknownFileTypeFileName, "This is an unknown file type")

        val originalFileList = listOf(validFile1, validFile2, validFile3, unknownFileTypeFileName)
        val renamedToProcessingList =
            listOf("$validFile1.processing", "$validFile2.processing", "$validFile3.processing")

        val totalFilesOnSftpServerBeforeProcessing = getSftpOutboundDirectoryFileNames()
        // Files are present on the server before processing
        assertThat(totalFilesOnSftpServerBeforeProcessing)
            .hasSize(4)
            .containsAll(originalFileList)

        // When
        processPrintResponsesBatchJob.pollAndProcessPrintResponses()

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(renamedToProcessingList)).isFalse
            assertThat(hasFilesPresentInOutboundDirectory(listOf(unknownFileTypeFileName))).isTrue
            assertThat(getSftpOutboundDirectoryFileNames()).hasSize(1)
        }
    }

    @Test
    fun `should update print requests for print responses`() {
        // Given
        val batchId1 = aValidBatchId()
        val batchId2 = aValidBatchId()
        val certificate1 = buildCertificate(
            printRequests = listOf(
                buildPrintRequest(
                    batchId = batchId1,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = Status.SENT_TO_PRINT_PROVIDER,
                            eventDateTime = Instant.now().minusSeconds(2).truncatedTo(ChronoUnit.SECONDS)
                        )
                    )
                )
            )
        )
        val certificate2 = buildCertificate(
            printRequests = listOf(
                buildPrintRequest(
                    batchId = batchId2,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = Status.SENT_TO_PRINT_PROVIDER,
                            eventDateTime = Instant.now().minusSeconds(2).truncatedTo(ChronoUnit.SECONDS)
                        )
                    )
                )
            )
        )
        val requestId1 = certificate1.printRequests.first().requestId!!
        certificateRepository.save(certificate1)
        certificateRepository.save(certificate2)

        val fileName = "status-20220928235441000.json"
        val printResponses = getPrintResponses(batchId1, batchId2, requestId1)
        writeContentToRemoteOutBoundDirectory(fileName, objectMapper.writeValueAsString(printResponses))

        val expectedStatuses1 = certificate1.printRequests.first().statusHistory.toMutableList()
        expectedStatuses1.addAll(
            listOf(
                buildPrintRequestStatus(
                    status = Status.RECEIVED_BY_PRINT_PROVIDER,
                    eventDateTime = printResponses.batchResponses[0].timestamp.toInstant(),
                    message = null
                ),
                buildPrintRequestStatus(
                    status = Status.VALIDATED_BY_PRINT_PROVIDER,
                    eventDateTime = printResponses.printResponses[0].timestamp.toInstant(),
                    message = null
                ),
                buildPrintRequestStatus(
                    status = Status.IN_PRODUCTION,
                    eventDateTime = printResponses.printResponses[1].timestamp.toInstant(),
                    message = null
                ),
                buildPrintRequestStatus(
                    status = Status.NOT_DELIVERED,
                    eventDateTime = printResponses.printResponses[2].timestamp.toInstant(),
                    message = printResponses.printResponses[2].message
                ),
            )
        )
        val expectedStatuses2 = certificate2.printRequests.first().statusHistory.toMutableList()
        expectedStatuses2.add(
            buildPrintRequestStatus(
                status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                eventDateTime = printResponses.batchResponses[0].timestamp.toInstant(),
                message = printResponses.batchResponses[1].message
            )
        )
        val expectedCertificate1 = certificate1.deepCopy()
        expectedCertificate1.status = Status.NOT_DELIVERED
        expectedCertificate1.printRequests.first().statusHistory = expectedStatuses1

        val expectedCertificate2 = certificate2.deepCopy()
        expectedCertificate2.status = Status.PENDING_ASSIGNMENT_TO_BATCH
        expectedCertificate2.printRequests.first().batchId = null
        expectedCertificate2.printRequests.first().statusHistory = expectedStatuses2

        // When
        processPrintResponsesBatchJob.pollAndProcessPrintResponses()

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            val generatedFields = arrayOf(".*dateCreated", ".*createdBy", ".*version", ".*id", ".*sanitizedSurname")

            val actualCertificate1 = certificateRepository.findById(certificate1.id!!).get()
            assertThat(actualCertificate1).usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*generatedFields)
                .ignoringCollectionOrder()
                .isEqualTo(expectedCertificate1)

            val actualCertificate2 = certificateRepository.findById(certificate2.id!!).get()
            assertThat(actualCertificate2).usingRecursiveComparison()
                .ignoringFieldsMatchingRegexes(*generatedFields, "printRequests.requestId")
                .ignoringCollectionOrder()
                .isEqualTo(expectedCertificate2)
            assertThat(actualCertificate2.printRequests.first().requestId)
                .isNotEqualTo(certificate2.printRequests.first().requestId)
                .containsPattern(Regex("^[a-f\\d]{24}$").pattern)
        }
    }

    private fun getPrintResponses(
        batchId1: String,
        batchId2: String,
        requestId1: String
    ): PrintResponses {
        val timestamp1 = Instant.now().truncatedTo(ChronoUnit.SECONDS).atOffset(ZoneOffset.UTC)
        val timestamp2 = timestamp1.plusSeconds(2)
        val timestamp3 = timestamp2.plusSeconds(2)
        val timestamp4 = timestamp3.plusSeconds(2)

        return buildPrintResponses(
            batchResponses = listOf(
                buildBatchResponse(
                    batchId = batchId1,
                    status = BatchResponse.Status.SUCCESS,
                    timestamp = timestamp1
                ),
                buildBatchResponse(
                    batchId = batchId2,
                    status = BatchResponse.Status.FAILED,
                    message = "$batchId2 batch failed",
                    timestamp = timestamp1
                ),
            ),
            printResponses = listOf(
                buildPrintResponse(
                    requestId = requestId1,
                    statusStep = PrintResponse.StatusStep.PROCESSED,
                    status = PrintResponse.Status.SUCCESS,
                    timestamp = timestamp2
                ),
                buildPrintResponse(
                    requestId = requestId1,
                    statusStep = PrintResponse.StatusStep.IN_PRODUCTION,
                    status = PrintResponse.Status.SUCCESS,
                    timestamp = timestamp3
                ),
                buildPrintResponse(
                    requestId = requestId1,
                    statusStep = PrintResponse.StatusStep.NOT_DELIVERED,
                    status = PrintResponse.Status.FAILED,
                    message = "$requestId1 dispatch failed",
                    timestamp = timestamp4
                )
            )
        )
    }
}
