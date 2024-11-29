package uk.gov.dluhc.printapi.testsupport.testdata.zip

import uk.gov.dluhc.printapi.config.LocalStackContainerConfiguration.Companion.VCA_TARGET_BUCKET
import uk.gov.dluhc.printapi.service.PhotoLocation
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference

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

fun anotherPhotoArn(
    bucket: String = aPhotoBucket(),
    path: String = anotherPhotoBucketPath()
) = "arn:aws:s3:::$bucket/$path"

fun aPhotoBucket() = VCA_TARGET_BUCKET
fun aPhotoBucketPath() = "E99999999/6407b6158f529a11713a1e5c/certificate-photos/0d77b2ad-64e7-4aa9-b4de-d58380392962_certificate-photo-1.png"
fun anotherPhotoBucketPath() = "E99999999/2304v5134f529a11713a1e6a/certificate-photos/0d21c6de-72d4-5aa2-c4da-c33456252922_certificate-photo-1.png"
fun anotherPhotoBucketPath2() = "E99999999/dhea5vf8gocm1bj9s7mvs940/certificate-photos/v6afark2-o4hx-wr3c-s6gt-a6n2dy2x1iuo_certificate-photo-2.png"

fun aPhotoZipPath() = "05372cf5339447b39f98b248c2217b9f-635abede7c432c0aaeeeba47.png"

fun anAedPhotoUrl(
    eroId: String = aValidEroId(),
    sourceReference: String = aValidSourceReference()
) = "http://localhost:8080/eros/$eroId/anonymous-elector-documents/photo?applicationId=$sourceReference"

fun aVacPhotoUrl(
    eroId: String = aValidEroId(),
    sourceReference: String = aValidSourceReference()
) = "http://localhost:8080/eros/$eroId/certificates/photo?applicationId=$sourceReference"
