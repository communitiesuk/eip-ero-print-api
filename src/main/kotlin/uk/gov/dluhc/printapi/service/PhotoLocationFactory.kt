package uk.gov.dluhc.printapi.service

import org.springframework.stereotype.Component

@Component
class PhotoLocationFactory {

    fun create(batchId: String, requestId: String, photoArn: String): PhotoLocation {
        val s3Location = parseS3Arn(photoArn)
        val zipPhotoPath = "$batchId-$requestId.${s3Location.path.substringAfterLast('.')}"
        return PhotoLocation(zipPhotoPath, s3Location.bucket, s3Location.path)
    }
}
