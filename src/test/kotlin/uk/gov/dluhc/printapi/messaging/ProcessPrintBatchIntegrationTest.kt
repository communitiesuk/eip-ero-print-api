package uk.gov.dluhc.printapi.messaging

import com.jcraft.jsch.ChannelSftp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.integration.file.remote.InputStreamCallback
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration.Companion.S3_BUCKET_CONTAINING_PHOTOS
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_REQUEST_UPLOAD_PATH
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.service.ProcessPrintBatchService
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.zip.ZipInputStream

// TODO - This should be test for the Listener that processes a SQS message
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ProcessPrintBatchIntegrationTest : IntegrationTest() {
    @Autowired
    private lateinit var printBatchService: ProcessPrintBatchService

    @Test
    fun `should process batch`() {
        // Given
        val printDetailsId = UUID.randomUUID()
        val batchId = aValidBatchId()
        val requestId = aValidRequestId()
        val s3ResourceContents = "S3 Object Contents"
        val s3Bucket = S3_BUCKET_CONTAINING_PHOTOS
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

        // save print requests in DynamoDB
        val details = buildPrintDetails(
            id = printDetailsId,
            batchId = batchId,
            requestId = requestId,
            eroWelsh = buildElectoralRegistrationOffice(),
            photoLocation = "arn:aws:s3:::$s3Bucket/$s3Path"
        )
        printDetailsRepository.save(details)
        assertThat(filterListForName(batchId)).isEmpty()

        // When
        printBatchService.processBatch(batchId)

        // Then
        val sftpDirectoryList = filterListForName(batchId)
        assertThat(sftpDirectoryList).hasSize(1)
        verifySftpZipFile(sftpDirectoryList, batchId, requestId, s3ResourceContents)
        val processedPrintDetails = printDetailsRepository.get(printDetailsId)
        assertThat(processedPrintDetails.status).isEqualTo(Status.SENT_TO_PRINT_PROVIDER)
    }

    private fun verifySftpZipFile(
        sftpDirectoryList: List<ChannelSftp.LsEntry>,
        batchId: String,
        requestId: String,
        s3ResourceContents: String
    ) {
        val filename = sftpDirectoryList[0].filename
        assertThat(filename).matches("$batchId-\\d{17}-1.zip")
        val expectedPhotoPathInZip = "$batchId-$requestId.png"
        sftpTemplate.get(
            "$PRINT_REQUEST_UPLOAD_PATH/$filename",
            (
                InputStreamCallback { stream ->
                    run {
                        val zipFile = ZipInputStream(stream)
                        val psvFile = zipFile.nextEntry
                        assertThat(psvFile).isNotNull
                        assertThat(psvFile!!.name).matches("$batchId-\\d{17}-1.psv")
                        val psvContents = String(zipFile.readBytes())
                        assertThat(psvContents).contains(expectedPhotoPathInZip)
                        val photoFile = zipFile.nextEntry
                        assertThat(photoFile).isNotNull
                        assertThat(photoFile!!.name).isEqualTo(expectedPhotoPathInZip)
                        val photoFileContents = String(zipFile.readBytes())
                        assertThat(photoFileContents).isEqualTo(s3ResourceContents)
                    }
                }
                )
        )
    }

    private fun filterListForName(batchId: String) =
        sftpTemplate.list(PRINT_REQUEST_UPLOAD_PATH).filter { lsEntry -> lsEntry.filename.contains(batchId) }
}
