package uk.gov.dluhc.printapi.testsupport

/**
 * Generated pre-signed GET S3 url in tests would look something like this
 * [X-Amz-Expires=120] is derived from property [`s3: *-upload-expiry-duration`] in seconds.
 * <pre>
 http://{hostname}:{port}/{source_bucket_name}/E59733855_63778af635c4d92d1b1a7163_HappyFace.png?
 X-Amz-Algorithm=AWS4-HMAC-SHA256
 &X-Amz-Date=20221118T133906Z
 &X-Amz-SignedHeaders=content-type;host
 &X-Amz-Expires=119
 &X-Amz-Credential=test%2F20221118%2Fus-east-1%2Fs3%2Faws4_request
 &X-Amz-Signature=b781de7ca0ccdb013e6d1da81406d19bbdca2cccdb0570b5c73603781b930904)
 * </pre>
 */
fun matchingPreSignedAwsS3GetUrl(key: String): Regex =
    Regex(
        ".*/$key" +
            "\\?X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Date=.*" +
            "&X-Amz-SignedHeaders=host" +
            "&X-Amz-Expires=.*" +
            "&X-Amz-Credential=.*" +
            "&X-Amz-Signature=.*"
    )

fun buildS3Arn(bucketName: String, s3ObjectKey: String): String {
    return "arn:aws:s3:::$bucketName/$s3ObjectKey"
}
