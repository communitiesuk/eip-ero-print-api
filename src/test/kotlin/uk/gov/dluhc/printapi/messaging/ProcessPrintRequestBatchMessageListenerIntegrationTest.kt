package uk.gov.dluhc.printapi.messaging

import com.jcraft.jsch.ChannelSftp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import org.springframework.integration.file.remote.InputStreamCallback
import org.springframework.test.context.transaction.TestTransaction
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration.Companion.VCA_TARGET_BUCKET
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_REQUEST_UPLOAD_PATH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.ASSIGNED_TO_BATCH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildProcessPrintRequestBatchMessage
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream
import javax.transaction.Transactional

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProcessPrintRequestBatchMessageListenerIntegrationTest : IntegrationTest() {

    @Transactional
    @ParameterizedTest
    @NullSource
    @CsvSource("true", "false")
    fun `should process print request batch message`(isFromApplicationsApi: Boolean?) {
        // Given
        val batchId = aValidBatchId()
        val requestId = aValidRequestId()
        val s3ResourceContents = "S3 Object Contents"
        val s3Bucket = VCA_TARGET_BUCKET
        val s3Path =
            "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"

        // add resource to S3
        val s3Resource = s3ResourceContents.encodeToByteArray()
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Path)
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )

        // save certificates in MySQL
        var certificate = buildCertificate(
            status = ASSIGNED_TO_BATCH,
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3Path",
            printRequests = mutableListOf(
                buildPrintRequest(
                    batchId = batchId,
                    requestId = requestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                )
            )
        )
        certificate = certificateRepository.save(certificate)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(filterListForName(batchId)).isEmpty()

        // add message to queue for processing
        val payload = buildProcessPrintRequestBatchMessage(batchId = batchId, isFromApplicationsApi = isFromApplicationsApi)

        // When
        TestTransaction.start()
        sqsMessagingTemplate.convertAndSend(processPrintRequestBatchQueueName, payload)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // Then
        TestTransaction.start()
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val sftpDirectoryList = filterListForName(batchId)
            assertThat(sftpDirectoryList).hasSize(1)
            verifySftpZipFile(sftpDirectoryList, batchId, listOf(requestId), s3ResourceContents)
            val processedCertificate = certificateRepository.findById(certificate.id!!).get()
            assertThat(processedCertificate.status).isEqualTo(SENT_TO_PRINT_PROVIDER)
            if (isFromApplicationsApi == true) {
                assertUpdateApplicationStatisticsMessageSent(certificate.sourceReference!!)
            } else {
                assertUpdateStatisticsMessageSent(certificate.sourceReference!!)
            }
        }
    }

    @Test
    @Transactional
    fun `should process print request batch message with multiple print requests pending in same batch`() {
        // Given
        val batchId = aValidBatchId()
        val firstRequestId = aValidRequestId()
        val secondRequestId = aValidRequestId()
        val s3ResourceContents = "S3 Object Contents"
        val s3Bucket = VCA_TARGET_BUCKET
        val s3Path =
            "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"

        // add resource to S3
        val s3Resource = s3ResourceContents.encodeToByteArray()
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Path)
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )

        // save certificates in MySQL
        var certificate = buildCertificate(
            status = ASSIGNED_TO_BATCH,
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3Path",
            printRequests = mutableListOf(
                buildPrintRequest(
                    batchId = batchId,
                    requestId = firstRequestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                ),
                buildPrintRequest(
                    batchId = batchId,
                    requestId = secondRequestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                )
            )
        )
        certificate = certificateRepository.save(certificate)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        assertThat(filterListForName(batchId)).isEmpty()

        // add message to queue for processing
        val payload = buildProcessPrintRequestBatchMessage(batchId = batchId)

        // When
        TestTransaction.start()
        sqsMessagingTemplate.convertAndSend(processPrintRequestBatchQueueName, payload)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // Then
        TestTransaction.start()
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val sftpDirectoryList = filterListForName(batchId)
            assertThat(sftpDirectoryList).hasSize(1)
            verifySftpZipFile(sftpDirectoryList, batchId, listOf(firstRequestId, secondRequestId), s3ResourceContents)
            val processedCertificate = certificateRepository.findById(certificate.id!!).get()
            assertThat(processedCertificate.status).isEqualTo(SENT_TO_PRINT_PROVIDER)
            assertUpdateStatisticsMessageSent(certificate.sourceReference!!)
        }
    }

    private fun verifySftpZipFile(
        sftpDirectoryList: List<ChannelSftp.LsEntry>,
        batchId: String,
        requestIdList: List<String>,
        s3ResourceContents: String
    ) {
        val filename = sftpDirectoryList[0].filename
        val printRequestCount = requestIdList.size
        assertThat(filename).matches("$batchId-\\d{17}-$printRequestCount.zip")
        sftpInboundTemplate.get(
            "$PRINT_REQUEST_UPLOAD_PATH/$filename",
            (
                InputStreamCallback { stream ->
                    run {
                        val zipFile = ZipInputStream(stream)
                        val psvFile = zipFile.nextEntry
                        assertThat(psvFile).isNotNull
                        assertThat(psvFile!!.name).matches("$batchId-\\d{17}-$printRequestCount.psv")
                        val psvContents = String(zipFile.readBytes())
                        val actualPhotoNames = mutableListOf<String>()
                        val expectedPhotoNames = mutableListOf<String>()
                        for (requestId in requestIdList) {
                            val expectedPhotoPathInZip = "$batchId-$requestId.png"
                            assertThat(psvContents).contains(expectedPhotoPathInZip)
                            val photoFile = zipFile.nextEntry
                            assertThat(photoFile).isNotNull
                            actualPhotoNames.add(photoFile!!.name)
                            expectedPhotoNames.add(expectedPhotoPathInZip)
                        }
                        assertThat(actualPhotoNames).containsAll(expectedPhotoNames)
                        val photoFileContents = String(zipFile.readBytes())
                        assertThat(photoFileContents).isEqualTo(s3ResourceContents)
                    }
                }
                )
        )
    }

    private fun filterListForName(batchId: String) =
        sftpInboundTemplate.list(PRINT_REQUEST_UPLOAD_PATH).filter { lsEntry -> lsEntry.filename.contains(batchId) }
}
