package uk.gov.dluhc.printapi.testsupport

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.ByteArrayInputStream

fun S3Client.addCertificatePhotoToS3(bucket: String, path: String) {
    // add resource to S3
    val s3ResourceContents = "S3 Object Contents"
    val s3Resource = s3ResourceContents.encodeToByteArray()
    putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(path)
            .build(),
        RequestBody.fromInputStream(ByteArrayInputStream(s3Resource), s3Resource.size.toLong())
    )
}

fun S3Client.certificatePhotoExists(bucket: String, path: String): Boolean {
    return try {
        headObject(HeadObjectRequest.builder().bucket(bucket).key(path).build())
        true
    } catch (e: NoSuchKeyException) {
        false
    }
}
