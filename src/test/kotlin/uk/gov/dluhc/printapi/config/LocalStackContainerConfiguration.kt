package uk.gov.dluhc.printapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.Projection
import software.amazon.awssdk.services.dynamodb.model.ProjectionType
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.REQUEST_ID_INDEX_NAME
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.SOURCE_TYPE_GSS_CODE_INDEX_NAME
import uk.gov.dluhc.printapi.database.entity.PrintDetails.Companion.STATUS_BATCH_ID_INDEX_NAME
import java.net.InetAddress
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
                        "SERVICES" to "sqs, dynamodb",
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

    @Primary
    @Bean
    fun createS3BucketSettings(
        awsCredentialsProvider: AwsCredentialsProvider?
    ): S3Client {
        val s3Client = S3Client.builder()
            .endpointOverride(localStackContainer.getEndpointOverride())
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(DEFAULT_ACCESS_KEY_ID, DEFAULT_SECRET_KEY)
                )
            )
            .build()

        createS3BucketsRequiredForTesting(s3Client)

        return s3Client
    }

    private fun createS3BucketsRequiredForTesting(s3Client: S3Client) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(S3_BUCKET_CONTAINING_PHOTOS).build())
        } catch (ex: S3Exception) {
            // ignore
        }
    }

    /**
     * Uses the localstack container to configure the various services.
     *
     * @return a [LocalStackContainerSettings] bean encapsulating the various IDs etc of the configured container and services.
     */
    @Bean
    fun localStackContainerSqsSettings(
        applicationContext: ConfigurableApplicationContext,
        @Value("\${sqs.send-application-to-print-queue-name}") sendApplicationToPrintQueueName: String,
        @Value("\${sqs.process-print-request-batch-queue-name}") processPrintRequestBatchQueueName: String,
        @Value("\${sqs.process-print-response-file-queue-name}") processPrintResponseFileQueueName: String,
        @Value("\${sqs.process-print-response-queue-name}") processPrintResponsesQueueName: String
    ): LocalStackContainerSettings {
        val queueUrlSendApplicationToPrint = localStackContainer.createSqsQueue(sendApplicationToPrintQueueName)
        val queueUrlProcessPrintBatchRequest = localStackContainer.createSqsQueue(processPrintRequestBatchQueueName)
        val queueUrlProcessPrintResponseFile = localStackContainer.createSqsQueue(processPrintResponseFileQueueName)
        val queueUrlProcessPrintResponses = localStackContainer.createSqsQueue(processPrintResponsesQueueName)
        val apiUrl = "http://${localStackContainer.host}:${localStackContainer.getMappedPort(DEFAULT_PORT)}"

        TestPropertyValues.of(
            "cloud.aws.sqs.endpoint=$apiUrl"
        ).applyTo(applicationContext)

        return LocalStackContainerSettings(
            apiUrl = apiUrl,
            queueUrlSendApplicationToPrint = queueUrlSendApplicationToPrint,
            queueUrlProcessPrintBatchRequest = queueUrlProcessPrintBatchRequest,
            queueUrlProcessPrintResponseFile = queueUrlProcessPrintResponseFile,
            queueUrlProcessPrintResponses = queueUrlProcessPrintResponses
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
        val queueUrlProcessPrintResponses: String
    ) {
        val mappedQueueUrlSendApplicationToPrint: String = toMappedUrl(queueUrlSendApplicationToPrint, apiUrl)
        val mappedQueueUrlProcessPrintBatchRequest: String = toMappedUrl(queueUrlProcessPrintBatchRequest, apiUrl)
        val mappedQueueUrlProcessPrintResponseFile: String = toMappedUrl(queueUrlProcessPrintResponseFile, apiUrl)
        val mappedQueueUrlProcessPrintResponses: String = toMappedUrl(queueUrlProcessPrintResponses, apiUrl)

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

    @Primary
    @Bean
    fun testDynamoDbClient(
        testAwsCredentialsProvider: AwsCredentialsProvider,
        dbConfiguration: DynamoDbConfiguration
    ): DynamoDbClient {
        val dynamoDbClient = DynamoDbClient.builder()
            .credentialsProvider(testAwsCredentialsProvider)
            .endpointOverride(localStackContainer.getEndpointOverride())
            .build()

        createPrintDetailsTable(dynamoDbClient, dbConfiguration.printDetailsTableName)
        return dynamoDbClient
    }

    private fun createPrintDetailsTable(dynamoDbClient: DynamoDbClient, tableName: String) {
        if (dynamoDbClient.listTables().tableNames().contains(tableName)) {
            dynamoDbClient.deleteTable { it.tableName(tableName) }
        }

        val attributeDefinitions: MutableList<AttributeDefinition> = mutableListOf(
            attributeDefinition("id"),
            attributeDefinition("requestId"),
            attributeDefinition("sourceType"),
            attributeDefinition("gssCode"),
            attributeDefinition("status"),
            attributeDefinition("batchId")
        )

        val keySchema: MutableList<KeySchemaElement> = mutableListOf(partitionKey("id"))

        val requestIdIndexSchema = globalSecondaryIndex(REQUEST_ID_INDEX_NAME, "requestId")
        val sourceTypeGssCodeIndexSchema =
            globalSecondaryIndex(SOURCE_TYPE_GSS_CODE_INDEX_NAME, "sourceType", "gssCode")
        val statusBatchIdIndexSchema = globalSecondaryIndex(STATUS_BATCH_ID_INDEX_NAME, "status", "batchId")

        val request: CreateTableRequest = CreateTableRequest.builder()
            .tableName(tableName)
            .keySchema(keySchema)
            .globalSecondaryIndexes(requestIdIndexSchema, sourceTypeGssCodeIndexSchema, statusBatchIdIndexSchema)
            .attributeDefinitions(attributeDefinitions)
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build()

        dynamoDbClient.createTable(request)
    }

    private fun globalSecondaryIndex(
        indexName: String,
        partitionKey: String,
        sortKey: String? = null
    ): GlobalSecondaryIndex {
        val indexKeySchema: MutableList<KeySchemaElement> =
            mutableListOf(partitionKey(partitionKey))

        if (sortKey != null) {
            indexKeySchema.add(sortKey(sortKey))
        }

        return GlobalSecondaryIndex.builder()
            .indexName(indexName)
            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(0L).writeCapacityUnits(0L).build())
            .keySchema(indexKeySchema)
            .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
            .build()
    }

    private fun attributeDefinition(name: String): AttributeDefinition =
        AttributeDefinition.builder().attributeName(name).attributeType("S").build()

    private fun partitionKey(name: String): KeySchemaElement =
        KeySchemaElement.builder().attributeName(name).keyType(KeyType.HASH).build()

    private fun sortKey(name: String): KeySchemaElement =
        KeySchemaElement.builder().attributeName(name).keyType(KeyType.RANGE).build()

    private fun GenericContainer<*>.getEndpointOverride(): URI {
        val ipAddress = InetAddress.getByName(host).hostAddress
        val mappedPort = getMappedPort(DEFAULT_PORT)
        return URI("http://$ipAddress:$mappedPort")
    }
}
