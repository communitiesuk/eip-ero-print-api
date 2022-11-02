package uk.gov.dluhc.printapi.config

import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.WiremockService

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "PT5M")
internal abstract class IntegrationTest {

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var wireMockService: WiremockService

    @Autowired
    protected lateinit var sqsMessagingTemplate: QueueMessagingTemplate

    @Autowired
    protected lateinit var dynamoDbClient: DynamoDbClient

    @Autowired
    protected lateinit var dynamoDbConfiguration: DynamoDbConfiguration

    @Autowired
    protected lateinit var printDetailsRepository: PrintDetailsRepository

    @Value("\${sqs.send-application-to-print-queue-name}")
    protected lateinit var sendApplicationToPrintQueueName: String

    @Value("\${sqs.process-print-request-batch-queue-name}")
    protected lateinit var processPrintRequestBatchQueueName: String

    @BeforeEach
    fun clearLogAppender() {
        TestLogAppender.reset()
    }

    @BeforeEach
    fun clearDatabase() {
        clearTable(dynamoDbConfiguration.printDetailsTableName)
    }

    @BeforeEach
    fun resetWireMock() {
        wireMockService.resetAllStubsAndMappings()
    }

    companion object {
        val localStackContainer = LocalStackContainerConfiguration.getInstance()
    }

    protected fun clearTable(tableName: String, partitionKey: String = "id", sortKey: String? = null) {
        val response = dynamoDbClient.scan(ScanRequest.builder().tableName(tableName).build())
        response.items().forEach {
            val keys = mutableMapOf<String, AttributeValue>(
                partitionKey to AttributeValue.builder().s(it[partitionKey]!!.s()).build(),
            )

            if (sortKey != null) {
                keys[sortKey] = AttributeValue.builder().s(it[partitionKey]!!.s()).build()
            }

            val deleteRequest = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(keys)
                .build()

            dynamoDbClient.deleteItem(deleteRequest)
        }
    }
}
