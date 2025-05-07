package uk.gov.dluhc.printapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.operations.SqsTemplate
import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.apache.sshd.sftp.client.SftpClient
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.core.io.ByteArrayResource
import org.springframework.dao.TransientDataAccessException
import org.springframework.data.repository.CrudRepository
import org.springframework.integration.file.FileHeaders.FILENAME
import org.springframework.integration.file.remote.session.CachingSessionFactory
import org.springframework.integration.file.remote.session.SessionFactory
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.support.MessageBuilder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.Tag
import uk.gov.dluhc.bankholidaysdataclient.BankHolidayDataClient
import uk.gov.dluhc.messagingsupport.MessageQueue
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_REQUEST_UPLOAD_PATH
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentSummaryRepository
import uk.gov.dluhc.printapi.database.repository.CertificateRepository
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.jobs.BatchPrintRequestsJob
import uk.gov.dluhc.printapi.jobs.FinalRetentionPeriodDataRemovalJob
import uk.gov.dluhc.printapi.jobs.InitialRetentionPeriodDataRemovalJob
import uk.gov.dluhc.printapi.jobs.ProcessPrintResponsesBatchJob
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.messaging.models.RemoveCertificateMessage
import uk.gov.dluhc.printapi.messaging.stubs.UpdateApplicationStatisticsMessageListenerStub
import uk.gov.dluhc.printapi.messaging.stubs.UpdateStatisticsMessageListenerStub
import uk.gov.dluhc.printapi.service.AedDataRetentionService
import uk.gov.dluhc.printapi.service.BankHolidaysDataService
import uk.gov.dluhc.printapi.service.CertificateSummarySearchService
import uk.gov.dluhc.printapi.service.SftpService
import uk.gov.dluhc.printapi.service.aed.AnonymousElectorDocumentSearchService
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.WiremockService
import uk.gov.dluhc.printapi.testsupport.emails.LocalstackEmailMessage
import uk.gov.dluhc.printapi.testsupport.emails.LocalstackEmailMessagesSentClient
import java.io.File
import java.nio.charset.Charset
import java.time.Clock
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * Base class used to bring up the entire Spring ApplicationContext
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [IntegrationTest.IntegrationTestConfiguration::class],
)
@ActiveProfiles("integration-test")
@AutoConfigureWebTestClient(timeout = "PT5M")
internal abstract class IntegrationTest {

    @Autowired
    protected lateinit var webTestClient: WebTestClient

    @Autowired
    protected lateinit var wireMockService: WiremockService

    @Autowired
    protected lateinit var localstackEmailMessagesSentClient: LocalstackEmailMessagesSentClient

    @Autowired
    protected lateinit var sqsTemplate: SqsTemplate

    @Autowired
    protected lateinit var sftpService: SftpService

    @Autowired
    @Qualifier("sftpInboundTemplate")
    protected lateinit var sftpInboundTemplate: SftpRemoteFileTemplate

    @Autowired
    @Qualifier("sftpOutboundTemplate")
    protected lateinit var sftpOutboundTemplate: SftpRemoteFileTemplate

    @Autowired
    protected lateinit var processPrintResponsesBatchJob: ProcessPrintResponsesBatchJob

    @Autowired
    protected lateinit var initialRetentionPeriodDataRemovalJob: InitialRetentionPeriodDataRemovalJob

    @Autowired
    protected lateinit var finalRetentionPeriodDataRemovalJob: FinalRetentionPeriodDataRemovalJob

    @Autowired
    protected lateinit var batchPrintRequestsJob: BatchPrintRequestsJob

    @Autowired
    protected lateinit var s3Client: S3Client

    @Autowired
    protected lateinit var bankHolidayDataClient: BankHolidayDataClient

    @Autowired
    protected lateinit var clock: Clock

    @Autowired
    protected lateinit var processPrintResponseFileMessageQueue: MessageQueue<ProcessPrintResponseFileMessage>

    @Autowired
    protected lateinit var processPrintResponseMessageQueue: MessageQueue<ProcessPrintResponseMessage>

    @Autowired
    protected lateinit var removeCertificateMessageQueue: MessageQueue<RemoveCertificateMessage>

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Value("\${sqs.send-application-to-print-queue-name}")
    protected lateinit var sendApplicationToPrintQueueName: String

    @Value("\${sqs.process-print-request-batch-queue-name}")
    protected lateinit var processPrintRequestBatchQueueName: String

    @Value("\${sqs.process-print-response-file-queue-name}")
    protected lateinit var processPrintResponseFileQueueName: String

    @Value("\${sqs.application-removed-queue-name}")
    protected lateinit var applicationRemovedQueueName: String

    @Value("\${sqs.remove-certificate-queue-name}")
    protected lateinit var removeCertificateQueueName: String

    @Value("\${sqs.trigger-voter-card-statistics-update-queue-name}")
    protected lateinit var triggerStatisticsUpdateQueueName: String

    @Value("\${sqs.trigger-application-statistics-update-queue-name}")
    protected lateinit var triggerApplicationStatisticsUpdateQueueName: String

    @Autowired
    protected lateinit var certificateRepository: CertificateRepository

    @Autowired
    protected lateinit var temporaryCertificateRepository: TemporaryCertificateRepository

    @Autowired
    protected lateinit var anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository

    @Autowired
    protected lateinit var anonymousElectorDocumentSummaryRepository: AnonymousElectorDocumentSummaryRepository

    @Autowired
    protected lateinit var testDeliveryRepository: TestDeliveryRepository

    @Autowired
    protected lateinit var testAnonymousElectorDocumentDeliveryRepository: TestAnonymousElectorDocumentDeliveryRepository

    @Autowired
    protected lateinit var testAddressRepository: TestAddressRepository

    @Autowired
    protected lateinit var testPrintRequestRepository: TestPrintRequestRepository

    @Autowired
    protected lateinit var anonymousElectorDocumentSearchService: AnonymousElectorDocumentSearchService

    @Autowired
    protected lateinit var certificateSummarySearchService: CertificateSummarySearchService

    @Autowired
    protected lateinit var bankHolidaysDataService: BankHolidaysDataService

    @Autowired
    protected lateinit var aedDataRetentionService: AedDataRetentionService

    @Autowired
    protected lateinit var cacheManager: CacheManager

    @Autowired
    protected lateinit var updateStatisticsMessageListenerStub: UpdateStatisticsMessageListenerStub

    @Autowired
    protected lateinit var updateApplicationStatisticsMessageListenerStub: UpdateApplicationStatisticsMessageListenerStub

    @Value("\${caching.time-to-live}")
    protected lateinit var timeToLive: Duration

    @BeforeEach
    fun clearLogAppender() {
        TestLogAppender.reset()
    }

    @BeforeEach
    fun clearMessagesFromStubs() {
        updateStatisticsMessageListenerStub.clear()
        updateApplicationStatisticsMessageListenerStub.clear()
    }

    @BeforeEach
    fun clearSftpUploadDirectory() {
        getSftpInboundDirectoryFileNames()
            .forEach { path -> sftpInboundTemplate.remove("$PRINT_REQUEST_UPLOAD_PATH/$path") }
    }

    @BeforeEach
    fun clearSftpDownloadDirectory() {
        getSftpOutboundDirectoryFileNames()
            .forEach { path -> sftpOutboundTemplate.remove("$PRINT_RESPONSE_DOWNLOAD_PATH/$path") }
    }

    @BeforeEach
    fun resetWireMock() {
        wireMockService.resetAllStubsAndMappings()
    }

    @BeforeEach
    @AfterEach
    fun clearDatabase() {
        clearRepository(certificateRepository, "certificateRepository")
        clearRepository(temporaryCertificateRepository, "temporaryCertificateRepository")
        clearRepository(anonymousElectorDocumentRepository, "anonymousElectorDocumentRepository")
        cacheManager.getCache(UK_BANK_HOLIDAYS_CACHE)?.clear()
    }

    private fun clearRepository(repository: CrudRepository<*, *>, repoName: String) {
        try {
            repository.deleteAll()
        } catch (tdae: TransientDataAccessException) {
            logger.warn("exception while cleaning up db with `$repoName.deleteAll()`", tdae)
            repository.deleteAll()
        }
    }

    companion object {
        val mysqlContainerConfiguration: MySQLContainerConfiguration = MySQLContainerConfiguration.getInstance()
        val localStackContainer = LocalStackContainerConfiguration.getInstance()
        val sftpContainer = SftpContainerConfiguration.getInstance()
        const val LOCAL_SFTP_OUTBOUND_TEST_DIRECTORY = "src/test/resources/sftp/local/OutBound"
        const val ERO_ID = "some-city-council"
    }

    @TestConfiguration
    class IntegrationTestConfiguration {
        @Bean
        @Primary
        fun testSftpSessionFactory(properties: SftpProperties): SessionFactory<SftpClient.DirEntry> {
            val factory = DefaultSftpSessionFactory(true)
            factory.setHost(properties.host)
            factory.setPort(sftpContainer.getMappedPort(SftpContainerConfiguration.DEFAULT_SFTP_PORT))
            factory.setUser(properties.user)
            factory.setPrivateKey(ByteArrayResource(properties.privateKey.encodeToByteArray()))
            factory.setAllowUnknownKeys(true)
            return CachingSessionFactory(factory)
        }
    }

    protected fun getSftpOutboundDirectoryFileNames() =
        getSftpDirectoryFileNames(sftpOutboundTemplate, PRINT_RESPONSE_DOWNLOAD_PATH)

    protected fun hasFilesPresentInOutboundDirectory(filenames: List<String>) =
        getSftpOutboundDirectoryFileNames().containsAll(filenames)

    protected fun writeContentToRemoteOutBoundDirectory(fileName: String, fileContent: String): String? {
        val remoteFilenamePath = sftpOutboundTemplate.send(
            MessageBuilder
                .withPayload(IOUtils.toInputStream(fileContent, Charset.defaultCharset()))
                .setHeader(FILENAME, fileName)
                .build()
        )
        logger.info { "remote file written to: $remoteFilenamePath" }
        return remoteFilenamePath
    }

    protected fun writeFileToRemoteOutBoundDirectory(fileName: String) {
        sftpOutboundTemplate.send(
            MessageBuilder
                .withPayload(File("$LOCAL_SFTP_OUTBOUND_TEST_DIRECTORY/$fileName"))
                .setHeader(FILENAME, fileName)
                .build()
        )
    }

    protected fun assertEmailSent(expected: LocalstackEmailMessage) {
        val expectedEmailBodyRegex = Regex(expected.body.htmlPart!!)
        with(localstackEmailMessagesSentClient.getEmailMessagesSent()) {
            val foundMessage = messages.any {
                !it.timestamp.isBefore(expected.timestamp) &&
                    it.destination.toAddresses.toSet() == expected.destination.toAddresses.toSet() &&
                    it.subject == expected.subject &&
                    expectedEmailBodyRegex.matches(it.body.htmlPart!!) &&
                    it.body.textPart == expected.body.textPart &&
                    it.source == expected.source
            }
            Assertions.assertThat(foundMessage)
                .`as` { "failed to find expectedEmailMessage[$expected], in list of messages[$messages]" }
                .isTrue
        }
    }

    protected fun assertUpdateStatisticsMessageSent(applicationId: String) {
        val messages = updateStatisticsMessageListenerStub.getMessages()
        Assertions.assertThat(messages).isNotEmpty
        Assertions.assertThat(messages).anyMatch {
            it.voterCardApplicationId == applicationId
        }
    }

    protected fun assertUpdateApplicationStatisticsMessageSent(applicationId: String) {
        val messages = updateApplicationStatisticsMessageListenerStub.getMessages()
        Assertions.assertThat(messages).isNotEmpty
        Assertions.assertThat(messages).anyMatch {
            it.applicationId == applicationId
        }
    }

    protected fun assertNumberOfUpdateStatisticsMessagesSent(count: Int) {
        val messages = updateStatisticsMessageListenerStub.getMessages()
        Assertions.assertThat(messages).hasSize(count)
    }

    protected fun assertUpdateStatisticsMessageNotSent() {
        val messages = updateStatisticsMessageListenerStub.getMessages()
        Assertions.assertThat(messages).isEmpty()
    }

    protected fun getObjectFromS3(s3Path: String): S3ObjectData {
        val s3Bucket = LocalStackContainerConfiguration.VCA_TARGET_BUCKET
        val getObjectRequest = GetObjectRequest.builder().bucket(s3Bucket).key(s3Path).build()
        val objectBytes = s3Client.getObjectAsBytes(getObjectRequest).asByteArray()
        val objectResponse = s3Client.getObject(getObjectRequest).response()
        val objectTags =
            s3Client.getObjectTagging(GetObjectTaggingRequest.builder().bucket(s3Bucket).key(s3Path).build())
        return S3ObjectData(objectBytes, objectResponse.contentType(), objectTags.tagSet())
    }

    protected class S3ObjectData(
        val bytes: ByteArray,
        val contentType: String,
        val tags: List<Tag>,
    )

    private fun getSftpInboundDirectoryFileNames() =
        getSftpDirectoryFileNames(sftpInboundTemplate, PRINT_REQUEST_UPLOAD_PATH)

    private fun getSftpDirectoryFileNames(
        sftpTemplate: SftpRemoteFileTemplate,
        directory: String
    ): List<String> {
        return sftpTemplate.list(directory)
            .map { it.filename }
            .filterNot { path -> path.equals(".") }
            .filterNot { path -> path.equals("..") }
            .toList()
    }
}
