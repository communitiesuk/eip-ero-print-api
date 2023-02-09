package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class S3ArnParserKtTest {

    @Test
    fun `should parse S3 ARN with qualifier`() {
        // Given
        val photoArn =
            "arn:aws:s3:::dev-vca-api-vca-target-bucket/E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
        val expectedS3Location = S3Location(
            "dev-vca-api-vca-target-bucket",
            "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
        )

        // When
        val s3Location = parseS3Arn(photoArn)

        // Then
        assertThat(s3Location).isEqualTo(expectedS3Location)
    }

    @Test
    fun `should parse S3 ARN without qualifier`() {
        // Given
        val photoArn = "arn:aws:s3:::dev-vca-api-vca-target-bucket/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
        val expectedS3Location =
            S3Location("dev-vca-api-vca-target-bucket", "8a53a30ac9bae2ebb9b1239b-initial-photo-1.png")

        // When
        val s3Location = parseS3Arn(photoArn)

        // Then
        assertThat(s3Location).isEqualTo(expectedS3Location)
    }
}
