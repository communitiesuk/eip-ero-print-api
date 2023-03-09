package uk.gov.dluhc.printapi.jobs

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.addCertificatePhotoToS3
import uk.gov.dluhc.printapi.testsupport.certificatePhotoExists
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucketPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.anotherPhotoBucketPath
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class FinalRetentionPeriodDataRemovalJobIntegrationTest : IntegrationTest() {

    @Autowired
    private lateinit var testPrintRequestRepository: TestPrintRequestRepository

    @Test
    fun `should remove voter card certificate final retention period data`() {
        // Given
        val s3Bucket = LocalStackContainerConfiguration.S3_BUCKET_CONTAINING_PHOTOS
        val s3PathPhoto1 = aPhotoBucketPath()
        val certificate1 = buildCertificate(
            sourceReference = "6407b6158f529a11713a1e5c",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1),
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathPhoto1"
        )
        val s3PathPhoto2 = anotherPhotoBucketPath()
        val certificate2 = buildCertificate(
            sourceReference = "2304v5134f529a11713a1e6a",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1),
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathPhoto2"
        )
        val certificate3 = buildCertificate(finalRetentionRemovalDate = LocalDate.now()) // should not be removed until tomorrow
        val certificate4 = buildCertificate(finalRetentionRemovalDate = LocalDate.now().plusDays(1)) // should not be removed
        certificateRepository.saveAll(listOf(certificate1, certificate2, certificate3, certificate4))

        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathPhoto1)
        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathPhoto2)
        TestLogAppender.reset()

        // When
        finalRetentionPeriodDataRemovalJob.removeVoterCardFinalRetentionPeriodData()

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(certificateRepository.findById(certificate1.id!!)).isEmpty
            assertThat(certificateRepository.findById(certificate2.id!!)).isEmpty
            assertThat(certificateRepository.findById(certificate3.id!!)).isNotEmpty
            assertThat(certificateRepository.findById(certificate4.id!!)).isNotEmpty
            assertThat(testPrintRequestRepository.findById(certificate1.printRequests[0].id!!)).isEmpty
            assertThat(testPrintRequestRepository.findById(certificate2.printRequests[0].id!!)).isEmpty
            assertThat(testPrintRequestRepository.findById(certificate3.printRequests[0].id!!)).isNotEmpty
            assertThat(testPrintRequestRepository.findById(certificate4.printRequests[0].id!!)).isNotEmpty
            assertThat(s3Client.certificatePhotoExists(s3Bucket, s3PathPhoto1)).isFalse
            assertThat(s3Client.certificatePhotoExists(s3Bucket, s3PathPhoto2)).isFalse
            assertThat(TestLogAppender.hasLog("Found 2 certificates with sourceType VOTER_CARD to remove", Level.INFO)).isTrue
        }
    }
}

@Repository
interface TestPrintRequestRepository : JpaRepository<PrintRequest, UUID>
