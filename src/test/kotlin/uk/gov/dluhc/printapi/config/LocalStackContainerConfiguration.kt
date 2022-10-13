package uk.gov.dluhc.printapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import java.net.URI

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
                    DockerImageName.parse("localstack/localstack:1.1.0")
                ).withEnv(
                    mapOf(
                        "SERVICES" to "sqs",
                        "AWS_DEFAULT_REGION" to DEFAULT_REGION,
                    )
                )
                    .withReuse(true)
                    .withExposedPorts(DEFAULT_PORT)
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

    /**
     * Uses the localstack container to configure the various services.
     *
     * @return a [LocalStackContainerSettings] bean encapsulating the various IDs etc of the configured container and services.
     */
    @Bean
    fun localStackContainerSqsSettings(
        applicationContext: ConfigurableApplicationContext,
        @Value("\${sqs.send-application-to-print-queue-name}") sendApplicationToPrintQueueName: String
    ): LocalStackContainerSettings {
        val queueUrlSendApplicationToPrint = localStackContainer.createSqsQueue(sendApplicationToPrintQueueName)
        val apiUrl = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}"

        TestPropertyValues.of(
            "cloud.aws.sqs.endpoint=$apiUrl"
        ).applyTo(applicationContext)

        return LocalStackContainerSettings(
            apiUrl = apiUrl,
            queueUrlSendApplicationToPrint = queueUrlSendApplicationToPrint,
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
        val queueUrlSendApplicationToPrint: String
    ) {
        val mappedQueueUrlSendApplicationToPrint: String = toMappedUrl(queueUrlSendApplicationToPrint, apiUrl)

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
}
