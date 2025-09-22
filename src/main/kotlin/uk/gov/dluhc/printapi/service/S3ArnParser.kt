package uk.gov.dluhc.printapi.service

import software.amazon.awssdk.arns.Arn

fun parseS3Arn(s3Arn: String): S3Location {
    val s3Photo = Arn.fromString(s3Arn)
    val bucket = s3Photo.resource().resourceType().get()
    val path = createPath(s3Photo)
    return S3Location(bucket, path)
}

private fun createPath(s3Photo: Arn): String =
    if (s3Photo.resource().qualifier().isPresent) {
        "${s3Photo.resource().resource()}/${s3Photo.resource().qualifier().get()}"
    } else {
        s3Photo.resource().resource()
    }

data class S3Location(
    val bucket: String,
    val path: String
)
