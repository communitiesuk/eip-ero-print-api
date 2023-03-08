package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucket
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucketPath
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit

internal class RemoveCertificateMessageListenerTest : IntegrationTest() {

    @Test
    fun `should remove certificate`() {
        // Given
        val certificate = buildCertificate()
        certificateRepository.save(certificate)
        val certificateId = certificate.id!!
        val s3PhotoBucket = aPhotoBucket()
        val s3PhotoPath = aPhotoBucketPath()
        addCertificatePhotoToS3(s3PhotoBucket, s3PhotoPath)

        val payload = RemoveCertificateMessage(
            certificateId = certificateId,
            certificatePhotoArn = aPhotoArn()
        )

        // When
        sqsMessagingTemplate.convertAndSend(removeCertificateQueueName, payload)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(certificateRepository.findById(certificateId)).isEmpty
            assertThat(certificatePhotoExists(s3PhotoBucket, s3PhotoPath)).isFalse
        }
    }

    private fun addCertificatePhotoToS3(bucket: String, path: String) {
        // add resource to S3
        val s3ResourceContents = "S3 Object Contents"
        val s3Resource = s3ResourceContents.encodeToByteArray()
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build(),
            RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
        )
    }

    private fun certificatePhotoExists(bucket: String, path: String): Boolean {
        return try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build())
            true
        } catch (e: NoSuchKeyException) {
            false
        }
    }
}
