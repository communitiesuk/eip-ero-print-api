package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import uk.gov.dluhc.printapi.config.S3Properties
import java.net.URI
import java.net.URL
import java.time.Duration

@ExtendWith(MockitoExtension::class)
internal class S3PhotoServiceTest {

    @Mock
    private lateinit var s3Client: S3Client

    @Mock
    private lateinit var s3Presigner: S3Presigner

    @Mock
    private lateinit var s3Properties: S3Properties

    private lateinit var s3PhotoService: S3PhotoService

    companion object {
        private const val S3_CERTIFICATE_PHOTO_TARGET_BUCKET = "secure-certificate-photos"
        private const val CERTIFICATE_PHOTO_ACCESS_TIME_IN_SECONDS = 70L
        private const val CUSTOM_DOMAIN_FILE_PROXY_URL = "customurl.testing.erop.ierds.uk"
        private const val S3_QUERY_PARAMS = "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Date=20230313T143807Z&X-Amz-SignedHeaders=host&X-Amz-Expires=215000&X-Amz-Credential=AKIAA5AAAAA1AAA1AAAA%2F20230313%2Feu-west-2%2Fs3%2Faws4_request&X-Amz-Signature=aaa000aa0a0000000a0a000a0a0aa000aa00000000aaa00000000000a000aa00"
    }

    @BeforeEach
    fun setupService() {
        given(s3Properties.certificatePhotoAccessDuration).willReturn(Duration.ofSeconds(CERTIFICATE_PHOTO_ACCESS_TIME_IN_SECONDS))
        given(s3Properties.certificatePhotosTargetBucket).willReturn(S3_CERTIFICATE_PHOTO_TARGET_BUCKET)
        given(s3Properties.certificatePhotosTargetBucketProxyEndpoint).willReturn(CUSTOM_DOMAIN_FILE_PROXY_URL)

        s3PhotoService = S3PhotoService(
            s3Client,
            s3Presigner,
            s3Properties,
        )
    }

    @Test
    fun `should generate pre-signed URL to get certificate photo`() {
        // Given
        val bucketName = s3Properties.certificatePhotosTargetBucket
        val key = "gssCode/key"
        val s3Arn = "arn:aws:s3:::$bucketName/$key"
        val presignedUrl = "https://${s3Properties.certificatePhotosTargetBucket}/$key?$S3_QUERY_PARAMS"
        val transformedUrl = "https://${s3Properties.certificatePhotosTargetBucketProxyEndpoint}/$key?$S3_QUERY_PARAMS"

        val expectedPresignRequest: GetObjectPresignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(CERTIFICATE_PHOTO_ACCESS_TIME_IN_SECONDS))
            .getObjectRequest(
                GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()
            )
            .build()

        val presignedGetObjectRequest = mock<PresignedGetObjectRequest>()
        given(presignedGetObjectRequest.url()).willReturn(URL(presignedUrl))
        given(s3Presigner.presignGetObject(any<GetObjectPresignRequest>())).willReturn(presignedGetObjectRequest)
        val expectedUri = URI.create(transformedUrl)

        // When
        val location = s3PhotoService.generatePresignedGetCertificatePhotoUrl(s3Arn)

        // Then
        assertThat(location).isEqualTo(expectedUri)
        verify(s3Presigner).presignGetObject(expectedPresignRequest)
    }
}
