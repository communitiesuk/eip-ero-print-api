package uk.gov.dluhc.printapi.jobs

import jakarta.transaction.Transactional
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.test.context.transaction.TestTransaction
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration.Companion.VCA_TARGET_BUCKET
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

internal class BatchPrintRequestsJobIntegrationTest : IntegrationTest() {

    @Test
    @Transactional
    fun `should send statistics update messages once for each application in a batch`() {
        // Given
        val s3Resource = "s3ResourceContents".encodeToByteArray()
        val s3Bucket = VCA_TARGET_BUCKET
        val s3Prefix = "E09000007/0013a30ac9bae2ebb9b1239b"

        // Given - add resources to S3
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(VCA_TARGET_BUCKET)
                .key("$s3Prefix/photo1.png")
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(VCA_TARGET_BUCKET)
                .key("$s3Prefix/photo2.png")
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )

        // Given - save certificates to database
        val certificate1 = buildCertificate(
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3Prefix/photo1.png",
            printRequests = listOf(
                buildPrintRequest(
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH,
                        )
                    )
                )
            )
        )
        val certificate2 = buildCertificate(
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3Prefix/photo2.png",
            printRequests = listOf(
                buildPrintRequest(
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = PrintRequestStatus.Status.PENDING_ASSIGNMENT_TO_BATCH,
                        )
                    )
                )
            )
        )
        certificateRepository.save(certificate1)
        certificateRepository.save(certificate2)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // When
        TestTransaction.start()
        batchPrintRequestsJob.run()
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // Then
        // One message will be sent for each certificate when the batch is taken off the queue and processed
        // Messages should not be sent during the job itself
        await.pollDelay(5, TimeUnit.SECONDS).atMost(8, TimeUnit.SECONDS).untilAsserted {
            assertUpdateApplicationStatisticsMessageSent(certificate1.sourceReference!!)
            assertUpdateApplicationStatisticsMessageSent(certificate2.sourceReference!!)
            assertNumberOfUpdateApplicationStatisticsMessagesSent(2)
        }
    }
}
