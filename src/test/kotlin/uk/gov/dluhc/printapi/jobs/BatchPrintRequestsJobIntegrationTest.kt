package uk.gov.dluhc.printapi.jobs

import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.test.context.transaction.TestTransaction
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration.Companion.S3_BUCKET_CONTAINING_PHOTOS
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.transaction.Transactional

internal class BatchPrintRequestsJobIntegrationTest : IntegrationTest() {

    @Test
    @Transactional
    fun `should send statistics update messages for each application in a batch`() {
        // Given
        val s3Resource = "s3ResourceContents".encodeToByteArray()
        val s3Bucket = S3_BUCKET_CONTAINING_PHOTOS
        val s3Prefix = "E09000007/0013a30ac9bae2ebb9b1239b"

        // Given - add resources to S3
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(S3_BUCKET_CONTAINING_PHOTOS)
                .key("$s3Prefix/photo1.png")
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(S3_BUCKET_CONTAINING_PHOTOS)
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

        // Clear messages from the queue in order to make the test valid
        //
        // Saving the certificates to the repository above will have triggered some
        // statistics update messages, but these aren't the ones we want to test for.
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertUpdateStatisticsMessageSent(certificate1.sourceReference!!)
            assertUpdateStatisticsMessageSent(certificate2.sourceReference!!)
        }
        updateStatisticsMessageListenerStub.clear()

        // When
        TestTransaction.start()
        batchPrintRequestsJob.run()
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // Then
        TestTransaction.start()
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertUpdateStatisticsMessageSent(certificate1.sourceReference!!)
            assertUpdateStatisticsMessageSent(certificate2.sourceReference!!)
        }
    }
}
