package uk.gov.dluhc.printapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI
import java.time.Duration

@Configuration
class S3Configuration(
    @Value("\${s3.localEndpointOverride:#{null}}") val endpointOverride: String?,
) {

    @Bean
    @Profile("!local")
    fun s3Client(): S3Client = S3Client.builder().build()

    @Bean
    @Profile("local")
    fun localS3Client(awsCredentialsProvider: AwsCredentialsProvider): S3Client =
        S3Client.builder()
            .endpointOverride(URI.create(endpointOverride ?: throw Exception("'sqs.localEndpointOverride' must be defined if running locally")))
            .credentialsProvider(awsCredentialsProvider)
            .forcePathStyle(true)
            .build()

    @Bean
    @Profile("!local")
    fun S3Presigner(): S3Presigner = S3Presigner.builder().build()

    @Bean
    @Profile("local")
    fun localS3Presigner(awsCredentialsProvider: AwsCredentialsProvider): S3Presigner =
        S3Presigner.builder()
            .endpointOverride(URI.create(endpointOverride ?: throw Exception("'sqs.localEndpointOverride' must be defined if running locally")))
            .credentialsProvider(awsCredentialsProvider)
            .build()
}

@ConfigurationProperties(prefix = "s3")
data class S3Properties(
    val vcaTargetBucket: String,
    val vcaTargetBucketProxyEndpoint: String,
    val certificatePhotoAccessDuration: Duration,
    val temporaryCertificateAccessDuration: Duration,
    val aedAccessDuration: Duration,
    val bankHolidaysBucket: String,
    val bankHolidaysBucketObjectKey: String,
)
