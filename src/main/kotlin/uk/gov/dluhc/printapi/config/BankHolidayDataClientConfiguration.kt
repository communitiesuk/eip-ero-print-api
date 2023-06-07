package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.bankholidaysdataclient.BankHolidayDataClient
import uk.gov.dluhc.bankholidaysdataclient.S3V2ApiBankHolidayDataClient

@Configuration
class BankHolidayDataClientConfiguration(
    private val s3Client: S3Client,
    private val s3Properties: S3Properties,
) {
    @Bean
    fun bankHolidayDataClient(): BankHolidayDataClient {
        return S3V2ApiBankHolidayDataClient(
            s3V2Client = s3Client,
            s3BucketName = s3Properties.bankHolidaysBucket,
            s3ObjectKey = s3Properties.bankHolidaysBucketObjectKey,
        )
    }
}
