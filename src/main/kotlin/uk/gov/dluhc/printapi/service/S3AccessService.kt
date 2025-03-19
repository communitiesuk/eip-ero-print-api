package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder
import software.amazon.awssdk.core.exception.SdkException
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
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

@Service
class S3AccessService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val s3Properties: S3Properties
) {

    private val bucketsToProxyEndpoints = mapOf(
        s3Properties.vcaTargetBucket to s3Properties.vcaTargetBucketProxyEndpoint
    )

    /**
     * Generates a pre-signed URL to the certificate photo in S3 (via a proxy) based on the provided S3 arn.
     */
    fun generatePresignedGetCertificatePhotoUrl(s3arn: String): URI {
        return generateGetResourceUrl(s3arn, s3Properties.certificatePhotoAccessDuration)
    }

    /**
     * Removes the document with the given ARN from the S3 bucket.
     */
    fun removeDocument(s3Arn: String) {
        try {
            with(parseS3Arn(s3Arn)) {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(path).build())
                logger.debug { "Deleted object with S3 arn [$s3Arn]" }
            }
        } catch (e: SdkException) {
            logger.warn { "Unable to delete object with S3 arn [$s3Arn] due to error [${e.cause?.message ?: e.cause}]" }
            throw e
        }
    }

    fun uploadTemporaryCertificate(
        gssCode: String,
        applicationId: String,
        fileName: String,
        contents: ByteArray,
    ): URI = uploadDownloadablePdf(gssCode, applicationId, fileName, contents, s3Properties.temporaryCertificateAccessDuration)

    fun uploadAed(
        gssCode: String,
        applicationId: String,
        fileName: String,
        contents: ByteArray,
    ): URI = uploadDownloadablePdf(gssCode, applicationId, fileName, contents, s3Properties.aedAccessDuration)

    /**
     * Uploads a downloadable PDF - either a Temporary Certificate or an AED - to S3 and
     * returns a presigned URL to access the object.
     */
    private fun uploadDownloadablePdf(
        gssCode: String,
        applicationId: String,
        fileName: String,
        contents: ByteArray,
        accessDuration: Duration
    ): URI {
        val s3Path = "$gssCode/$applicationId/$fileName"
        putObjectToTargetBucket(
            key = s3Path,
            data = contents,
            contentType = MediaType.APPLICATION_PDF,
            tagObjectForDeletion = true
        )
        val s3Arn = buildS3Arn(s3Path)
        return generateGetResourceUrl(s3Arn, accessDuration)
    }

    private fun putObjectToTargetBucket(
        key: String,
        data: ByteArray,
        contentType: MediaType,
        tagObjectForDeletion: Boolean
    ) {
        val bucket = s3Properties.vcaTargetBucket
        try {
            s3Client.putObject(
                buildPutObjectRequest(
                    key = key,
                    bucket = bucket,
                    contentType = contentType,
                    tagObjectForDeletion = tagObjectForDeletion,
                ),
                RequestBody.fromBytes(data)
            )
            logger.debug { "Put object to S3 bucket [$bucket] with path [$key]" }
        } catch (e: SdkException) {
            logger.warn { "Unable to put object to S3 bucket [$bucket] with path [$key] due to error [${e.cause?.message ?: e.cause}]" }
            throw e
        }
    }

    private fun buildPutObjectRequest(
        key: String,
        bucket: String,
        contentType: MediaType?,
        tagObjectForDeletion: Boolean
    ): PutObjectRequest {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)

        if (contentType != null) {
            request.contentType(contentType.toString())
        }

        if (tagObjectForDeletion) {
            val deletionTag = Tag.builder().key("ToDelete").value("True").build()
            request.tagging(Tagging.builder().tagSet(deletionTag).build())
        }
        return request.build()
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
        return transformS3ResourceUrl(req, s3Resource.bucket)
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

    private fun buildS3Arn(s3ObjectKey: String): String {
        return "arn:aws:s3:::${s3Properties.vcaTargetBucket}/$s3ObjectKey"
    }
}
