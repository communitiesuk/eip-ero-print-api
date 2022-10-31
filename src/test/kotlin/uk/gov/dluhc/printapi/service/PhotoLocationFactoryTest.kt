package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PhotoLocationFactoryTest {

    private val factory = PhotoLocationFactory()

    @Test
    fun `should populate photo ARN with qualifier`() {
        // Given
        val batchId = "05372cf5339447b39f98b248c2217b9f"
        val requestId = "635abe7eda1fd8437a09fb74"
        val photoArn = "arn:aws:s3:::dev-vca-api-vca-target-bucket/E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"

        // When
        val photoLocation = factory.create(batchId, requestId, photoArn)

        // Then
        assertThat(photoLocation).isEqualTo(
            PhotoLocation(
                "05372cf5339447b39f98b248c2217b9f-635abe7eda1fd8437a09fb74.png",
                "dev-vca-api-vca-target-bucket",
                "E09000007/0013a30ac9bae2ebb9b1239b/0d77b2ad-64e7-4aa9-b4de-d58380392962/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
            )
        )
    }

    @Test
    fun `should populate photo ARN without qualifier`() {
        // Given
        val photoArn = "arn:aws:s3:::dev-vca-api-vca-target-bucket/8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
        val batchId = "05372cf5339447b39f98b248c2217b9f"
        val requestId = "635abede7c432c0aaeeeba47"

        // When
        val photoLocation = factory.create(batchId, requestId, photoArn)

        // Then
        assertThat(photoLocation).isEqualTo(
            PhotoLocation(
                "05372cf5339447b39f98b248c2217b9f-635abede7c432c0aaeeeba47.png",
                "dev-vca-api-vca-target-bucket",
                "8a53a30ac9bae2ebb9b1239b-initial-photo-1.png"
            )
        )
    }
}
