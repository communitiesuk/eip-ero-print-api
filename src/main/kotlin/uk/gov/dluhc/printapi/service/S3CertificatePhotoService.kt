package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import uk.gov.dluhc.printapi.database.entity.Certificate

private val logger = KotlinLogging.logger {}

@Service
class S3CertificatePhotoService(
    private val s3Client: S3Client
) {
    /**
     * Removes a Certificate's photos from S3.
     */
    fun removeCertificatePhoto(certificate: Certificate) {
        val certificateS3Arn = certificate.printRequests[0].photoLocationArn!!
        val s3Location = parseS3Arn(certificateS3Arn)
        // TODO - error handling if photo does exist

        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3Location.bucket).key(s3Location.path).build())

        logger.info { "Deleted certificate photo with s3arn [$certificateS3Arn]" }
    }
}
