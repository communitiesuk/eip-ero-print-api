package uk.gov.dluhc.printapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ClassPathResource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.ses.SesClient
import uk.gov.dluhc.printapi.testsupport.buildS3Arn
import java.net.InetAddress
import java.net.URI

private val logger = KotlinLogging.logger {}

/**
 * Configuration class exposing beans for the LocalStack (AWS) environment.
 */
@Configuration
class LocalStackContainerConfiguration {

    companion object {
        private const val DEFAULT_REGION = "eu-west-2"
        const val DEFAULT_PORT = 4566
        const val DEFAULT_ACCESS_KEY_ID = "test"
        const val DEFAULT_SECRET_KEY = "test"
        const val S3_BUCKET_CONTAINING_PHOTOS = "localstack-vca-api-vca-target-bucket"

        val objectMapper = ObjectMapper()
        val localStackContainer: GenericContainer<*> = getInstance()
        private var container: GenericContainer<*>? = null

        /**
         * Creates and starts LocalStack configured with a basic (empty) SQS service.
         * Returns the container that can subsequently be used for further setup and configuration.
         */
        fun getInstance(): GenericContainer<*> {
            if (container == null) {
                container = GenericContainer(
                    DockerImageName.parse("localstack/localstack:1.2.0")
                ).withEnv(
                    mapOf(
                        "SERVICES" to "sqs, ses",
                        "AWS_DEFAULT_REGION" to DEFAULT_REGION,
                    )
                )
                    .withReuse(true)
                    .withExposedPorts(DEFAULT_PORT)
                    .withCreateContainerCmdModifier {
                        it.withName("print-api-integration-test-localstack")
                    }
                    .apply {
                        start()
                    }
            }

            return container!!
        }
    }

    @Bean
    fun awsBasicCredentialsProvider(): AwsCredentialsProvider =
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                DEFAULT_ACCESS_KEY_ID,
                DEFAULT_SECRET_KEY
            )
        )

    @Primary
    @Bean
    fun createS3BucketSettings(
        awsCredentialsProvider: AwsCredentialsProvider?,
        s3Properties: S3Properties
    ): S3Client {
        val s3Client = S3Client.builder()
            .endpointOverride(localStackContainer.getEndpointOverride())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(DEFAULT_ACCESS_KEY_ID, DEFAULT_SECRET_KEY)
                )
            )
            .build()

        createS3BucketsRequiredForTesting(s3Client = s3Client, s3Properties = s3Properties)
        createS3FileInBankHolidaysBucket(s3Client = s3Client, s3Properties = s3Properties)

        return s3Client
    }

    private fun createS3BucketsRequiredForTesting(
        s3Client: S3Client,
        s3Properties: S3Properties
    ) {
        try {
            with(s3Properties) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(certificatePhotosTargetBucket).build())
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bankHolidaysBucket).build())
            }
        } catch (ex: S3Exception) {
            // ignore
        }
    }

    private fun createS3FileInBankHolidaysBucket(
        s3Client: S3Client,
        s3Properties: S3Properties
    ) {
        with(s3Properties) {
            val bankHolidaysArn = buildS3Arn(bankHolidaysBucket, bankHolidaysBucketObjectKey)
            val testFileLocation = "bank-holidays/test-bank-holidays.json"
            val putObjectRequest =
                PutObjectRequest.builder()
                    .bucket(bankHolidaysBucket)
                    .key(bankHolidaysBucketObjectKey)
                    .build()
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(ClassPathResource(testFileLocation).file))
            logger.info { "Uploaded [$testFileLocation] to S3 bucket [$bankHolidaysBucket] with arn [$bankHolidaysArn]" }
        }
    }

    @Bean
    @Primary
    fun configureEmailIdentityAndExposeSesClient(
        awsBasicCredentialsProvider: AwsCredentialsProvider,
        emailClientProperties: EmailClientProperties,
    ): SesClient {
        localStackContainer.verifyEmailIdentity(emailClientProperties.sender)

        return SesClient.builder()
            .region(Region.of(DEFAULT_REGION))
            .credentialsProvider(awsBasicCredentialsProvider)
            .applyMutation { builder -> builder.endpointOverride(localStackContainer.getEndpointOverride()) }
            .build()
    }

    private fun GenericContainer<*>.verifyEmailIdentity(emailAddress: String) {
        val execInContainer = execInContainer(
            "awslocal", "ses", "verify-email-identity", "--email-address", emailAddress
        )
        if (execInContainer.exitCode == 0) {
            logger.info { "verified email identity: $emailAddress" }
        } else {
            logger.error { "failed to create email identity: $emailAddress" }
            logger.error { "failed to create email identity[stdout]: ${execInContainer.stdout}" }
            logger.error { "failed to create email identity[stderr]: ${execInContainer.stderr}" }
        }
    }

    /**
     * Uses the localstack container to configure the various services.
     *
     * @return a [LocalStackContainerSettings] bean encapsulating the various IDs etc of the configured container and services.
     */
    @Bean
    fun localStackContainerSettings(
        applicationContext: ConfigurableApplicationContext,
        @Value("\${sqs.send-application-to-print-queue-name}") sendApplicationToPrintQueueName: String,
        @Value("\${sqs.process-print-request-batch-queue-name}") processPrintRequestBatchQueueName: String,
        @Value("\${sqs.process-print-response-file-queue-name}") processPrintResponseFileQueueName: String,
        @Value("\${sqs.process-print-response-queue-name}") processPrintResponsesQueueName: String,
        @Value("\${sqs.application-removed-queue-name}") applicationRemovedQueueName: String,
        @Value("\${sqs.remove-certificate-queue-name}") removeCertificateQueueName: String
    ): LocalStackContainerSettings {
        val queueUrlSendApplicationToPrint = localStackContainer.createSqsQueue(sendApplicationToPrintQueueName)
        val queueUrlProcessPrintBatchRequest = localStackContainer.createSqsQueue(processPrintRequestBatchQueueName)
        val queueUrlProcessPrintResponseFile = localStackContainer.createSqsQueue(processPrintResponseFileQueueName)
        val queueUrlProcessPrintResponses = localStackContainer.createSqsQueue(processPrintResponsesQueueName)
        val queueUrlApplicationRemoved = localStackContainer.createSqsQueue(applicationRemovedQueueName)
        val queueUrlRemoveCertificate = localStackContainer.createSqsQueue(removeCertificateQueueName)
        localStackContainer.createSqsQueue("correlation-id-test-queue")

        val apiUrl = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}"

        TestPropertyValues.of(
            "cloud.aws.sqs.endpoint=$apiUrl",
        ).applyTo(applicationContext)

        return LocalStackContainerSettings(
            apiUrl = apiUrl,
            queueUrlSendApplicationToPrint = queueUrlSendApplicationToPrint,
            queueUrlProcessPrintBatchRequest = queueUrlProcessPrintBatchRequest,
            queueUrlProcessPrintResponseFile = queueUrlProcessPrintResponseFile,
            queueUrlProcessPrintResponses = queueUrlProcessPrintResponses,
            queueUrlApplicationRemoved = queueUrlApplicationRemoved,
            queueUrlRemoveCertificate = queueUrlRemoveCertificate
        )
    }

    private fun GenericContainer<*>.createSqsQueue(queueName: String): String {
        val execInContainer = execInContainer(
            "awslocal", "sqs", "create-queue", "--queue-name", queueName
        )
        return execInContainer.stdout.let {
            objectMapper.readValue(it, Map::class.java)
        }.let {
            it["QueueUrl"] as String
        }
    }

    data class LocalStackContainerSettings(
        val apiUrl: String,
        val queueUrlSendApplicationToPrint: String,
        val queueUrlProcessPrintBatchRequest: String,
        val queueUrlProcessPrintResponseFile: String,
        val queueUrlProcessPrintResponses: String,
        val queueUrlApplicationRemoved: String,
        val queueUrlRemoveCertificate: String,
    ) {
        val mappedQueueUrlSendApplicationToPrint: String = toMappedUrl(queueUrlSendApplicationToPrint, apiUrl)
        val mappedQueueUrlProcessPrintBatchRequest: String = toMappedUrl(queueUrlProcessPrintBatchRequest, apiUrl)
        val mappedQueueUrlProcessPrintResponseFile: String = toMappedUrl(queueUrlProcessPrintResponseFile, apiUrl)
        val mappedQueueUrlProcessPrintResponses: String = toMappedUrl(queueUrlProcessPrintResponses, apiUrl)
        val mappedQueueUrlApplicationRemoved: String = toMappedUrl(queueUrlApplicationRemoved, apiUrl)
        val mappedQueueUrlRemoveCertificate: String = toMappedUrl(queueUrlRemoveCertificate, apiUrl)
        val sesMessagesUrl = "$apiUrl/_localstack/ses"

        private fun toMappedUrl(rawUrlString: String, apiUrlString: String): String {
            val rawUrl = URI.create(rawUrlString)
            val apiUrl = URI.create(apiUrlString)
            return URI(
                rawUrl.scheme,
                rawUrl.userInfo,
                apiUrl.host,
                apiUrl.port,
                rawUrl.path,
                rawUrl.query,
                rawUrl.fragment
            ).toASCIIString()
        }
    }

    private fun GenericContainer<*>.getEndpointOverride(): URI {
        val ipAddress = InetAddress.getByName(host).hostAddress
        val mappedPort = getMappedPort(DEFAULT_PORT)
        return URI("http://$ipAddress:$mappedPort")
    }
}
