package uk.gov.dluhc.printapi.logging

import ch.qos.logback.classic.Level
import org.awaitility.kotlin.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.CORRELATION_ID
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SourceType.VOTER_CARD
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.ILoggingEventAssert.Companion.assertThat
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.getBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Integration tests that assert the correlation ID is correctly applied to log statements via the Interceptors and Aspects
 * in CorrelationIdMdcConfiguration
 *
 * The tests in this class assert the cross-cutting logging behaviour. They do not assert the behaviour or output of any
 * bean or code that is used to tests.
 */
internal class CorrelationIdMdcIntegrationTest : IntegrationTest() {

    @Nested
    inner class GetCertificateSummary {
        private val URI_TEMPLATE = "/eros/{ERO_ID}/certificates/applications/{APPLICATION_ID}"
        private val ERO_ID = "some-city-council"
        private val APPLICATION_ID = "7762ccac7c056046b75d4aa3"

        @BeforeEach
        fun setupWiremockStubs() {
            val eroResponse = buildElectoralRegistrationOfficeResponse(id = ERO_ID)
            wireMockService.stubCognitoJwtIssuerResponse()
            wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        }

        @Test
        fun `should service REST API call given no correlation-id header`() {
            // Given

            // When
            webTestClient.get()
                .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
                .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                // We don't care that the log message is because the certificate was not found. We care that the log message has a correlation ID
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Certificate for eroId = $ERO_ID with sourceType = $VOTER_CARD and sourceReference = $APPLICATION_ID not found",
                        Level.WARN
                    )
                ).hasAnyCorrelationId()
            }
        }

        @Test
        fun `should service REST API call given correlation-id header`() {
            // Given
            val expectedCorrelationId = UUID.randomUUID().toString().replace("-", "")

            // When
            webTestClient.get()
                .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
                .bearerToken(getBearerToken(eroId = ERO_ID, groups = listOf("ero-$ERO_ID", "ero-vc-admin-$ERO_ID")))
                .contentType(MediaType.APPLICATION_JSON)
                .header("x-correlation-id", expectedCorrelationId)
                .exchange()

            // Then
            await.atMost(3, TimeUnit.SECONDS).untilAsserted {
                // We don't care that the log message is because the certificate was not found. We care that the log message has a correlation ID
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Certificate for eroId = $ERO_ID with sourceType = $VOTER_CARD and sourceReference = $APPLICATION_ID not found",
                        Level.WARN
                    )
                ).hasCorrelationId(expectedCorrelationId)
            }
        }
    }

    @Nested
    inner class BatchPrintRequests {
        /* Assert that log statements for Batch Print Request processing all contain a consistent correlation ID
        Running the Batch Print Requests Job:
            Identifies Print Requests that are PENDING_ASSIGNMENT_TO_BATCH
            Updates the records with a batch ID and updates the status to ASSIGNED_TO_BATCH
            Submits a ProcessPrintRequestBatchMessage with the batch ID

        The ProcessPrintRequestBatchMessage listener:
            Streams images from S3
            Creates the zip file
            Uploads to SFTP
            Updates the status of the Print Request

        We expect a consistent correlation ID for all log messages. For example:

        2022-11-24 19:50:21.118 40dd2a03c7384affa779cdb6ad744f98 INFO 69738 --- [    Test worker] u.g.d.p.service.PrintRequestsService     : Looking for certificate Print Requests to assign to a new batch
        2022-11-24 19:50:21.143 40dd2a03c7384affa779cdb6ad744f98 INFO 69738 --- [    Test worker] u.g.d.p.s.CertificateBatchingService     : Certificate with id [cca679f5-5a3d-47f8-95f4-9b924fcab789] assigned to batch [cab0f871e14a48e6b5511422b12d5999]
        2022-11-24 19:50:21.409 40dd2a03c7384affa779cdb6ad744f98 INFO 69738 --- [enerContainer-6] .ProcessPrintRequestBatchMessageListener : Processing print batch request for batchId: cab0f871e14a48e6b5511422b12d5999
        2022-11-24 19:50:21.410 40dd2a03c7384affa779cdb6ad744f98 INFO 69738 --- [    Test worker] u.g.d.p.service.PrintRequestsService     : Batch [cab0f871e14a48e6b5511422b12d5999] containing 1 print requests submitted to queue
        2022-11-24 19:50:21.412 40dd2a03c7384affa779cdb6ad744f98 INFO 69738 --- [    Test worker] u.g.d.p.service.PrintRequestsService     : Completed batching certificate Print Requests
        2022-11-24 19:50:21.483 40dd2a03c7384affa779cdb6ad744f98 INFO 69738 --- [enerContainer-6] .ProcessPrintRequestBatchMessageListener : Successfully processed print request for batchId: cab0f871e14a48e6b5511422b12d5999
         */
        @Test
        fun `should run batch job and log consistent correlation id`() {
            // Given
            val certificateId = UUID.randomUUID()
            saveCertificate(certificateId)

            // When
            batchPrintRequestsJob.run()

            // Then
            await.atMost(10, TimeUnit.SECONDS).untilAsserted {
                val logEvent = TestLogAppender.getLogEventMatchingRegex(
                    "Looking for certificate Print Requests to assign to a new batch",
                    Level.INFO
                )
                assertThat(logEvent).isNotNull
                val expectedCorrelationId = logEvent!!.mdcPropertyMap[CORRELATION_ID]

                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Certificate ids \\[$certificateId\\] assigned to batch \\[.{32}\\]",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Batch \\[.{32}\\] containing .{1} print requests submitted to queue",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Completed batching certificate Print Requests",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Processing print batch request for batchId: .{32}",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Successfully processed print request for batchId: .{32}",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
            }
        }
    }

    @Nested
    inner class ProcessPrintResponsesBatch {
        /* Assert that log statements for Print Response processing all contain a consistent correlation ID
        Running the Process Print Responses Job:
            Looks for unprocessed Print Response files on the SFTP server
            Renames each file to indicate it's ready for processing
            Submits a ProcessPrintResponseFileMessage with the filename

        The ProcessPrintResponseFileMessageListener listener:
            Reads the file from SFTP into a Print Responses object
            Updates any BatchResponse's
            For each PrintResponse submit a ProcessPrintResponseMessage containing the request ID

        The ProcessPrintResponseMessageListener:
            Gets the Print Request from the database by it's request ID
            Updates the status

        We expect a consistent correlation ID for all log messages. For example:

        2022-11-25 10:16:56.740 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [    Test worker] .d.p.s.PrintResponseFileReadinessService : Finding matching print responses from directory: [EROP/Dev/OutBound]
        2022-11-25 10:16:56.749 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [    Test worker] .d.p.s.PrintResponseFileReadinessService : Found [1] unprocessed print responses
        2022-11-25 10:16:56.753 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [    Test worker] u.g.dluhc.printapi.service.SftpService   : Renaming [status-20220928235441000.json] to [status-20220928235441000.json.processing] in directory:[EROP/Dev/OutBound]
        2022-11-25 10:16:56.759 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [    Test worker] .d.p.s.PrintResponseFileReadinessService : Submitting SQS message for file: [1 of 1] with payload: ProcessPrintResponseFileMessage(directory=EROP/Dev/OutBound, fileName=status-20220928235441000.json.processing)
        2022-11-25 10:16:57.008 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [    Test worker] .d.p.s.PrintResponseFileReadinessService : Completed marking and processing all print response files from directory: [EROP/Dev/OutBound]
        2022-11-25 10:16:57.027 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [enerContainer-5] .ProcessPrintResponseFileMessageListener : Begin processing PrintResponse file [status-20220928235441000.json.processing] from directory [EROP/Dev/OutBound]
        2022-11-25 10:16:57.325 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [enerContainer-5] u.g.dluhc.printapi.service.SftpService   : Removing processed file [status-20220928235441000.json.processing] from directory [EROP/Dev/OutBound]
        2022-11-25 10:16:57.330 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [enerContainer-5] .ProcessPrintResponseFileMessageListener : Completed processing PrintResponse file [status-20220928235441000.json.processing] from directory [EROP/Dev/OutBound]
        2022-11-25 10:16:57.339 02e04607ab2f4babbcf963dd043d3bdf INFO 91901 --- [enerContainer-6] .p.m.ProcessPrintResponseMessageListener : Begin processing PrintResponse with requestId 63809618eb37192604a50a90
         */
        @Test
        fun `should run batch job and log consistent correlation id`() {
            // Given
            val certificateId = UUID.randomUUID()
            val certificate = saveCertificate(certificateId)
            val expectedRequestId = certificate.printRequests[0].requestId!!

            val statusUpdateFile = "status-20220928235441000.json"
            writeContentToRemoteOutBoundDirectory(
                statusUpdateFile,
                objectMapper.writeValueAsString(
                    buildPrintResponses(
                        batchResponses = emptyList(),
                        printResponses = listOf(
                            buildPrintResponse(
                                requestId = expectedRequestId
                            )
                        )
                    )
                )
            )

            // When
            processPrintResponsesBatchJob.pollAndProcessPrintResponses()

            // Then
            await.atMost(30, TimeUnit.SECONDS).untilAsserted {
                val logEvent = TestLogAppender.getLogEventMatchingRegex(
                    "Finding matching print responses from directory: \\[EROP/Dev/OutBound\\]",
                    Level.INFO
                )
                assertThat(logEvent).isNotNull
                val expectedCorrelationId = logEvent!!.mdcPropertyMap[CORRELATION_ID]

                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Renaming \\[$statusUpdateFile\\] to \\[$statusUpdateFile.processing\\] in directory:\\[EROP/Dev/OutBound\\]",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Submitting SQS message for file: \\[1 of 1\\] with payload: ProcessPrintResponseFileMessage\\(directory=EROP/Dev/OutBound, fileName=$statusUpdateFile.processing\\)",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Begin processing PrintResponse file \\[$statusUpdateFile.processing\\] from directory \\[EROP/Dev/OutBound\\]",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Removing processed file \\[$statusUpdateFile.processing\\] from directory \\[EROP/Dev/OutBound\\]",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
                assertThat(
                    TestLogAppender.getLogEventMatchingRegex(
                        "Begin processing PrintResponse with requestId $expectedRequestId",
                        Level.INFO
                    )
                ).hasCorrelationId(expectedCorrelationId)
            }
        }
    }

    private fun s3Resource(): String {
        val s3ResourceContents = "S3 Object Contents"
        val s3Bucket = LocalStackContainerConfiguration.S3_BUCKET_CONTAINING_PHOTOS
        val s3Path =
            "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"

        val s3Resource = s3ResourceContents.encodeToByteArray()
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Path)
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )

        return "arn:aws:s3:::$s3Bucket/$s3Path"
    }

    private fun saveCertificate(certificateId: UUID): Certificate {
        val certificate = buildCertificate(
            id = certificateId,
            status = Status.PENDING_ASSIGNMENT_TO_BATCH,
            printRequests = listOf(
                buildPrintRequest(
                    photoLocationArn = s3Resource()
                )
            )
        )
        return certificateRepository.save(certificate)
    }
}
