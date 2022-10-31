package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component
import software.amazon.awssdk.arns.Arn

@Component
class PhotoLocationFactory {

    fun create(batchId: String, requestId: String, photoArn: String): PhotoLocation {
        val s3Photo = Arn.fromString(photoArn)
        val bucket = s3Photo.resource().resourceType().get()
        val path = createPath(s3Photo)
        val zipPhotoPath = "$batchId-$requestId.${path.substringAfterLast('.')}"
        return PhotoLocation(zipPhotoPath, bucket, path)
    }

    private fun createPath(s3Photo: Arn): String =
        if (s3Photo.resource().qualifier().isPresent)
            "${s3Photo.resource().resource()}/${s3Photo.resource().qualifier().get()}"
        else s3Photo.resource().resource()
}
