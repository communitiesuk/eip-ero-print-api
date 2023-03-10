package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

private val logger = KotlinLogging.logger {}

@Service
class S3PhotoService(
    private val s3Client: S3Client
) {
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
}
