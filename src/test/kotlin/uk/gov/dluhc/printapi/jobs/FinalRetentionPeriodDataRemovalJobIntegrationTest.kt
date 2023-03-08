package uk.gov.dluhc.printapi.jobs

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.util.UUID

internal class FinalRetentionPeriodDataRemovalJobIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var testPrintRequestRepository: TestPrintRequestRepository

    @Test
    fun `should remove voter card final retention period data`() {
        // Given
        val s3Bucket = LocalStackContainerConfiguration.S3_BUCKET_CONTAINING_PHOTOS
        val s3Path1 = "E99999999/6407b6158f529a11713a1e5c/certificate-photos/0d77b2ad-64e7-4aa9-b4de-d58380392962_certificate-photo-1.png"
        val certificate1 = buildCertificate(
            sourceReference = "6407b6158f529a11713a1e5c",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1),
            printRequests = listOf(buildPrintRequest(photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3Path1"))
        )
        val s3Path2 = "E99999999/2304v5134f529a11713a1e6a/certificate-photos/0d21c6de-72d4-5aa2-c4da-c33456252922_certificate-photo-1.png"
        val certificate2 = buildCertificate(
            sourceReference = "2304v5134f529a11713a1e6a",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1),
            printRequests = listOf(buildPrintRequest(photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3Path2"))
        )
        val certificate3 = buildCertificate(finalRetentionRemovalDate = LocalDate.now()) // should not be removed until tomorrow
        val certificate4 = buildCertificate(finalRetentionRemovalDate = LocalDate.now().plusDays(1)) // should not be removed
        certificateRepository.saveAll(listOf(certificate1, certificate2, certificate3, certificate4))

        addCertificatePhotoToS3(s3Bucket, s3Path1)
        addCertificatePhotoToS3(s3Bucket, s3Path2)
        TestLogAppender.reset()

        // When
        finalRetentionPeriodDataRemovalJob.removeVoterCardFinalRetentionPeriodData()

        // Then
        assertThat(certificateRepository.findById(certificate1.id!!)).isEmpty
        assertThat(certificateRepository.findById(certificate2.id!!)).isEmpty
        assertThat(certificateRepository.findById(certificate3.id!!)).isNotEmpty
        assertThat(certificateRepository.findById(certificate4.id!!)).isNotEmpty
        assertThat(testPrintRequestRepository.findById(certificate1.printRequests[0].id!!)).isEmpty
        assertThat(testPrintRequestRepository.findById(certificate2.printRequests[0].id!!)).isEmpty
        assertThat(testPrintRequestRepository.findById(certificate3.printRequests[0].id!!)).isNotEmpty
        assertThat(testPrintRequestRepository.findById(certificate4.printRequests[0].id!!)).isNotEmpty
        assertThat(certificatePhotoExists(s3Bucket, s3Path1)).isFalse
        assertThat(certificatePhotoExists(s3Bucket, s3Path2)).isFalse
        assertThat(TestLogAppender.hasLog("Removed remaining data after final retention period from certificate with sourceReference ${certificate1.sourceReference}", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Removed remaining data after final retention period from certificate with sourceReference ${certificate2.sourceReference}", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Deleted certificate photo with s3arn [${certificate1.printRequests[0].photoLocationArn}]", Level.INFO)).isTrue
        assertThat(TestLogAppender.hasLog("Deleted certificate photo with s3arn [${certificate2.printRequests[0].photoLocationArn}]", Level.INFO)).isTrue
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

@Repository
interface TestPrintRequestRepository : JpaRepository<PrintRequest, UUID>
