package uk.gov.dluhc.printapi.testsupport.testdata.zip

import uk.gov.dluhc.printapi.service.PhotoLocation

fun buildPhotoLocation(
    zipPath: String = aPhotoZipPath(),
    sourceBucket: String = aPhotoBucket(),
    sourcePath: String = aPhotoBucketPath()
) = PhotoLocation(zipPath, sourceBucket, sourcePath)

fun aPhotoLocation(): PhotoLocation = buildPhotoLocation()

fun aPhotoLocationList(): List<PhotoLocation> = listOf(aPhotoLocation())

fun aPhotoArn(
    bucket: String = aPhotoBucket(),
    path: String = aPhotoBucketPath()
) = "arn:aws:s3:::$bucket/$path"

fun aPhotoBucket() = "dev-vca-api-vca-target-bucket"

fun aPhotoBucketPath() = "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"

fun aPhotoZipPath() = "05372cf5339447b39f98b248c2217b9f-635abede7c432c0aaeeeba47.png"
