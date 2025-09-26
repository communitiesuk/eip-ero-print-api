package uk.gov.dluhc.printapi.service

import org.apache.commons.io.IOUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.Tag
import software.amazon.awssdk.services.s3.model.Tagging
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import uk.gov.dluhc.printapi.config.S3Properties
import uk.gov.dluhc.printapi.testsupport.buildS3Arn
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import java.net.URI
import java.time.Duration

@ExtendWith(MockitoExtension::class)
internal class S3AccessServiceTest {

    @Mock
    private lateinit var s3Client: S3Client

    @Mock
    private lateinit var s3Presigner: S3Presigner

    @Mock
    private lateinit var s3Properties: S3Properties

    private lateinit var s3AccessService: S3AccessService

    companion object {
        private const val S3_CERTIFICATE_PHOTO_TARGET_BUCKET = "secure-certificate-photos"
        private const val CERTIFICATE_PHOTO_ACCESS_TIME_IN_SECONDS = 70L
        private const val TEMPORARY_CERTIFICATE_ACCESS_TIME_IN_SECONDS = 100L
        private const val CUSTOM_DOMAIN_FILE_PROXY_URL = "customurl.testing.erop.ierds.uk"
        private const val S3_QUERY_PARAMS =
            "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20230313T143807Z&X-Amz-SignedHeaders=host&X-Amz-Expires=215000&X-Amz-Credential=AKIAA5AAAAA1AAA1AAAA%2F20230313%2Feu-west-2%2Fs3%2Faws4_request&X-Amz-Signature=aaa000aa0a0000000a0a000a0a0aa000aa00000000aaa00000000000a000aa00"
    }

    @BeforeEach
    fun setupService() {
        given(s3Properties.vcaTargetBucket).willReturn(S3_CERTIFICATE_PHOTO_TARGET_BUCKET)
        given(s3Properties.vcaTargetBucketProxyEndpoint).willReturn(CUSTOM_DOMAIN_FILE_PROXY_URL)

        s3AccessService = S3AccessService(
            s3Client,
            s3Presigner,
            s3Properties,
        )
    }

    @Nested
    inner class GeneratePresignedGetCertificatePhotoUrl {
        @BeforeEach
        fun setupService() {
            given(s3Properties.certificatePhotoAccessDuration).willReturn(
                Duration.ofSeconds(
                    CERTIFICATE_PHOTO_ACCESS_TIME_IN_SECONDS
                )
            )
        }

        @Test
        fun `should generate pre-signed URL to get certificate photo`() {
            // Given
            val bucketName = s3Properties.vcaTargetBucket
            val key = "gssCode/key"
            val s3Arn = "arn:aws:s3:::$bucketName/$key"
            val presignedUrl = "https://${s3Properties.vcaTargetBucket}/$key?$S3_QUERY_PARAMS"
            val transformedUrl = "https://${s3Properties.vcaTargetBucketProxyEndpoint}/$key?$S3_QUERY_PARAMS"
            val expectedPresignRequest: GetObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(CERTIFICATE_PHOTO_ACCESS_TIME_IN_SECONDS))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(key).build())
                .build()
            val presignedGetObjectRequest = mock<PresignedGetObjectRequest>()
            given(presignedGetObjectRequest.url()).willReturn(URI.create(presignedUrl).toURL())
            given(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).willReturn(presignedGetObjectRequest)
            val expectedUri = URI.create(transformedUrl)

            // When
            val location = s3AccessService.generatePresignedGetCertificatePhotoUrl(s3Arn)

            // Then
            assertThat(location).isEqualTo(expectedUri)
            verify(s3Presigner).presignGetObject(expectedPresignRequest)
        }
    }

    @Nested
    inner class UploadTemporaryCertificate {
        private val gssCode = getRandomGssCode()
        private val applicationId = aValidSourceReference()
        private val fileName = "temporary-certificate"
        private val expectedKey = "$gssCode/$applicationId/$fileName"

        @BeforeEach
        fun setupService() {
            given(s3Properties.temporaryCertificateAccessDuration).willReturn(
                Duration.ofSeconds(
                    TEMPORARY_CERTIFICATE_ACCESS_TIME_IN_SECONDS
                )
            )

            val bucketName = s3Properties.vcaTargetBucket
            val presignedUrl = "https://$bucketName/$expectedKey?$S3_QUERY_PARAMS"
            val presignedGetObjectRequest = mock<PresignedGetObjectRequest>()
            given(presignedGetObjectRequest.url()).willReturn(URI.create(presignedUrl).toURL())
            given(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).willReturn(presignedGetObjectRequest)
        }

        @Test
        fun `should put object data into the S3 bucket, tagged as temporary object for deletion`() {
            // Given
            val bucketName = s3Properties.vcaTargetBucket
            val data = "test-data".toByteArray()

            // When
            s3AccessService.uploadTemporaryCertificate(
                gssCode = gssCode,
                applicationId = applicationId,
                fileName = fileName,
                contents = data,
            )

            // Then
            verify(s3Client).putObject(
                eq(
                    PutObjectRequest.builder()
                        .key(expectedKey)
                        .bucket(bucketName)
                        .contentType("application/pdf")
                        .tagging(Tagging.builder().tagSet(Tag.builder().key("ToDelete").value("True").build()).build())
                        .build()
                ),
                argThat<RequestBody> { requestBody -> requestBodyMatchesByteArray(requestBody, data) }
            )
        }

        @Test
        fun `should return pre-signed URL to get temporary certificate`() {
            // Given
            val bucketName = s3Properties.vcaTargetBucket
            val data = "test-data".toByteArray()
            val transformedUrl = "https://${s3Properties.vcaTargetBucketProxyEndpoint}/$expectedKey?$S3_QUERY_PARAMS"
            val expectedPresignRequest: GetObjectPresignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(TEMPORARY_CERTIFICATE_ACCESS_TIME_IN_SECONDS))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key(expectedKey).build())
                .build()
            val expectedUri = URI.create(transformedUrl)

            // When
            val location = s3AccessService.uploadTemporaryCertificate(
                gssCode = gssCode,
                applicationId = applicationId,
                fileName = fileName,
                contents = data,
            )

            // Then
            assertThat(location).isEqualTo(expectedUri)
            verify(s3Presigner).presignGetObject(expectedPresignRequest)
        }

        private fun requestBodyMatchesByteArray(requestBody: RequestBody, expectedByteArray: ByteArray): Boolean {
            val receivedByteArray = IOUtils.toByteArray(requestBody.contentStreamProvider().newStream())
            return expectedByteArray.contentEquals(receivedByteArray)
        }
    }

    @Nested
    inner class RemoveDocument {
        @Test
        fun `should remove document`() {
            // Given
            val bucketName = s3Properties.vcaTargetBucket
            val key = "test-key"
            val arn = buildS3Arn(bucketName, key)

            // When
            s3AccessService.removeDocument(arn)

            // Then
            verify(s3Client).deleteObject(
                DeleteObjectRequest.builder().key(key).bucket(bucketName).build()
            )
        }
    }
}
