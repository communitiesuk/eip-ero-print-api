package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Configuration {

    @Bean
    fun s3Client(): S3Client = S3Client.builder().build()
}
