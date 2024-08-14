package uk.gov.dluhc.printapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.time.Duration

@Configuration
class S3Configuration {

    @Bean
    fun s3Client(): S3Client = S3Client.builder().build()

    @Bean
    fun S3Presigner(): S3Presigner = S3Presigner.builder().build()
}

@ConfigurationProperties(prefix = "s3")
@ConstructorBinding
data class S3Properties(
    val vcaTargetBucket: String,
    val vcaTargetBucketProxyEndpoint: String,
    val certificatePhotoAccessDuration: Duration,
    val temporaryCertificateAccessDuration: Duration,
    val bankHolidaysBucket: String,
    val bankHolidaysBucketObjectKey: String,
)
