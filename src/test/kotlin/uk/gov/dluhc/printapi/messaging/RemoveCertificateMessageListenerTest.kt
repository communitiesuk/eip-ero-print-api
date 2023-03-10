package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.printapi.testsupport.addCertificatePhotoToS3
import uk.gov.dluhc.printapi.testsupport.certificatePhotoExists
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucket
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucketPath
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
        s3Client.addCertificatePhotoToS3(s3PhotoBucket, s3PhotoPath)

        val payload = RemoveCertificateMessage(
            certificateId = certificateId,
            certificatePhotoArn = aPhotoArn()
        )

        // When
        sqsMessagingTemplate.convertAndSend(removeCertificateQueueName, payload)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(certificateRepository.findById(certificateId)).isEmpty
            assertThat(s3Client.certificatePhotoExists(s3PhotoBucket, s3PhotoPath)).isFalse
        }
    }
}
