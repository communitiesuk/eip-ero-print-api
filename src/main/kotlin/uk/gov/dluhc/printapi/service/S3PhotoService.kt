package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import uk.gov.dluhc.printapi.config.S3Properties
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Service
class S3PhotoService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties
) {

    private val bucketsToProxyEndpoints = mapOf(
        s3Properties.certificatePhotosTargetBucket to s3Properties.certificatePhotosTargetBucketProxyEndpoint
    )

    /**
     * Generates a pre-signed URL to the certificate photo in S3 (via a proxy) based on the provided S3 arn.
     */
    fun generatePresignedGetCertificatePhotoUrl(s3arn: String): URI {
        return generateGetResourceUrl(s3arn, s3Properties.certificatePhotoAccessDuration)
    }

    /**
     * Removes the photo that is on the printed "Elector Document" (i.e. Certificate/AED) from S3.
     */
    fun removePhoto(photoS3Arn: String) {
        try {
            with(parseS3Arn(photoS3Arn)) {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build())
                logger.debug { "Deleted photo with S3 arn [$photoS3Arn]" }
            }
        } catch (e: SdkException) {
            logger.warn { "Unable to delete photo with S3 arn [$photoS3Arn] due to error [${e.cause?.message ?: e.cause}]" }
            throw e
        }
    }

    private fun generateGetResourceUrl(s3arn: String, accessDuration: Duration): URI {
        val s3Resource = parseS3Arn(s3arn)
        val getObjectRequest: GetObjectRequest = GetObjectRequest.builder()
            .bucket(s3Resource.bucket)
            .key(s3Resource.path)
            .build()

        val presignRequest: GetObjectPresignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(accessDuration)
            .getObjectRequest(getObjectRequest)
            .build()
        val req = s3Presigner.presignGetObject(presignRequest)
        return req.url().toURI()
        // TODO - EIP1-5838
        // return transformS3ResourceUrl(req, s3Resource.bucket)
    }

    private fun transformS3ResourceUrl(request: PresignedGetObjectRequest, bucketName: String): URI {
        with(request) {
            val preSignedS3Url = this.url()
            return UriComponentsBuilder.newInstance()
                .scheme("https")
                .host(bucketsToProxyEndpoints[bucketName])
                .path(preSignedS3Url.path)
                .query(preSignedS3Url.query)
                .build(true)
                .toUri()
        }
    }
}
