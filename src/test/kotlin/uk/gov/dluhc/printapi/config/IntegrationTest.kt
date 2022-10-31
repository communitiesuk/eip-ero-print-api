package uk.gov.dluhc.printapi.config

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.ChannelSftp.LsEntry
import io.awspring.cloud.messaging.core.QueueMessagingTemplate
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.integration.file.remote.session.CachingSessionFactory
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.s3.S3Client
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.REMOTE_PATH
import uk.gov.dluhc.printapi.database.repository.PrintDetailsRepository
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.WiremockService

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [IntegrationTest.IntegrationTestConfiguration::class],
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

    @Autowired
    protected lateinit var sftpTemplate: SftpRemoteFileTemplate

    @Autowired
    protected lateinit var s3Client: S3Client

    @Value("\${sqs.send-application-to-print-queue-name}")
    protected lateinit var sendApplicationToPrintQueueName: String

    @BeforeEach
    fun clearLogAppender() {
        TestLogAppender.reset()
    }

    @BeforeEach
    fun clearDatabase() {
        clearTable(dynamoDbConfiguration.printDetailsTableName)
    }

    @BeforeEach
    fun clearSftpUploadDirectory() {
        sftpTemplate.list(REMOTE_PATH)
            .map(LsEntry::getFilename)
            .filterNot { path -> path.equals(".") }
            .filterNot { path -> path.equals("..") }
            .forEach { path -> sftpTemplate.remove("$REMOTE_PATH/$path") }
    }

    @BeforeEach
    fun resetWireMock() {
        wireMockService.resetAllStubsAndMappings()
    }

    companion object {
        val localStackContainer = LocalStackContainerConfiguration.getInstance()
        val sftpContainer = SftpContainerConfiguration.getInstance()
    }

    @TestConfiguration
    class IntegrationTestConfiguration {
        @Bean
        @Primary
        fun testSftpSessionFactory(): SessionFactory<ChannelSftp.LsEntry> {
            val factory = DefaultSftpSessionFactory(true)
            factory.setHost(SftpContainerConfiguration.HOST)
            factory.setPort(sftpContainer.getMappedPort(SftpContainerConfiguration.DEFAULT_SFTP_PORT))
            factory.setUser(SftpContainerConfiguration.USER)
            factory.setPassword(SftpContainerConfiguration.PASSWORD)
            factory.setAllowUnknownKeys(true)
            return CachingSessionFactory(factory)
        }
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
