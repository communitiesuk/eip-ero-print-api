package uk.gov.dluhc.printapi.jobs

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.addCertificatePhotoToS3
import uk.gov.dluhc.printapi.testsupport.certificatePhotoExists
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoBucketPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.anotherPhotoBucketPath
import uk.gov.dluhc.printapi.testsupport.testdata.zip.anotherPhotoBucketPath2
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal class FinalRetentionPeriodDataRemovalJobIntegrationTest : IntegrationTest() {

    @Test
    fun `should remove voter card certificate final retention period data`() {
        // Given
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
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

        val temporaryCertificate1 = buildTemporaryCertificate(finalRetentionRemovalDate = LocalDate.now().minusDays(1))
        val temporaryCertificate2 = buildTemporaryCertificate(finalRetentionRemovalDate = LocalDate.now().minusDays(1))
        val temporaryCertificate3 = buildTemporaryCertificate(finalRetentionRemovalDate = LocalDate.now().plusDays(1))
        temporaryCertificateRepository.saveAll(listOf(temporaryCertificate1, temporaryCertificate2, temporaryCertificate3))

        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathPhoto1)
        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathPhoto2)
        TestLogAppender.reset()

        // When
        finalRetentionPeriodDataRemovalJob.removeVoterCardFinalRetentionPeriodData()

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(temporaryCertificateRepository.findById(temporaryCertificate1.id!!)).isEmpty
            assertThat(temporaryCertificateRepository.findById(temporaryCertificate2.id!!)).isEmpty
            assertThat(temporaryCertificateRepository.findById(temporaryCertificate3.id!!)).isNotEmpty

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

            assertThat(TestLogAppender.hasLog("Found 2 temporary certificates with sourceType VOTER_CARD to remove", Level.INFO)).isTrue
            assertThat(TestLogAppender.hasLog("Found 2 certificates with sourceType VOTER_CARD to remove", Level.INFO)).isTrue
        }
    }

    @Test
    fun `should remove anonymous elector document final retention period data`() {
        // Given
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
        val s3PathAedPhoto1 = aPhotoBucketPath()
        val anonymousElectorDocument1 = buildAnonymousElectorDocument(
            sourceReference = "6407b6158f529a11713a1e5c",
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto1",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1)
        )
        val s3PathAedPhoto2 = anotherPhotoBucketPath()
        val anonymousElectorDocument2 = buildAnonymousElectorDocument(
            sourceReference = "2304v5134f529a11713a1e6a",
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto2",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1)
        )
        val s3PathAedPhoto3 = anotherPhotoBucketPath2()
        val anonymousElectorDocument3 = buildAnonymousElectorDocument(
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto3",
            finalRetentionRemovalDate = LocalDate.now().plusDays(1)
        )
        anonymousElectorDocumentRepository.saveAll(listOf(anonymousElectorDocument1, anonymousElectorDocument2, anonymousElectorDocument3))

        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathAedPhoto1)
        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathAedPhoto2)
        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathAedPhoto3)
        TestLogAppender.reset()

        // When
        finalRetentionPeriodDataRemovalJob.removeAedFinalRetentionPeriodData()

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument1.id!!)).isEmpty
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument2.id!!)).isEmpty
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument3.id!!)).isNotEmpty
            assertThat(s3Client.certificatePhotoExists(s3Bucket, s3PathAedPhoto1)).isFalse
            assertThat(s3Client.certificatePhotoExists(s3Bucket, s3PathAedPhoto2)).isFalse
            assertThat(TestLogAppender.hasLog("Found 2 Anonymous Elector Documents with sourceType ANONYMOUS_ELECTOR_DOCUMENT to remove", Level.INFO)).isTrue
        }
    }

    @Test
    fun `should retain photos used by retained anonymous elector documents`() {
        // Given
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET

        val s3PathAedPhoto1 = aPhotoBucketPath()
        val anonymousElectorDocument1 = buildAnonymousElectorDocument(
            sourceReference = "6407b6158f529a11713a1e5c",
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto1",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1)
        )

        val anonymousElectorDocument2 = buildAnonymousElectorDocument(
            sourceReference = "2304v5134f529a11713a1e6a",
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto1",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1)
        )

        val s3PathAedPhoto2 = anotherPhotoBucketPath()
        val anonymousElectorDocument3 = buildAnonymousElectorDocument(
            sourceReference = "6409b6159f530a11714a2e6c",
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto2",
            finalRetentionRemovalDate = LocalDate.now().minusDays(1)
        )

        val anonymousElectorDocument4 = buildAnonymousElectorDocument(
            photoLocationArn = "arn:aws:s3:::$s3Bucket/$s3PathAedPhoto2",
            finalRetentionRemovalDate = LocalDate.now().plusDays(1)
        )

        anonymousElectorDocumentRepository.saveAll(
            listOf(
                anonymousElectorDocument1,
                anonymousElectorDocument2,
                anonymousElectorDocument3,
                anonymousElectorDocument4,
            )
        )

        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathAedPhoto1)
        s3Client.addCertificatePhotoToS3(s3Bucket, s3PathAedPhoto2)
        TestLogAppender.reset()

        // When
        finalRetentionPeriodDataRemovalJob.removeAedFinalRetentionPeriodData()

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument1.id!!)).isEmpty
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument2.id!!)).isEmpty
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument3.id!!)).isEmpty
            assertThat(anonymousElectorDocumentRepository.findById(anonymousElectorDocument4.id!!)).isNotEmpty

            // Photo 1 is only used by AEDs that are getting removed today, so delete
            // Photo 2 is used by AEDs that are retained during today's job, so retain
            assertThat(s3Client.certificatePhotoExists(s3Bucket, s3PathAedPhoto1)).isFalse
            assertThat(s3Client.certificatePhotoExists(s3Bucket, s3PathAedPhoto2)).isTrue

            assertThat(
                TestLogAppender.hasLog(
                    "Found 3 Anonymous Elector Documents with sourceType ANONYMOUS_ELECTOR_DOCUMENT to remove",
                    Level.INFO
                )
            ).isTrue
        }
    }
}
