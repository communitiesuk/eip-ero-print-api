package uk.gov.dluhc.printapi.messaging.service

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.springframework.messaging.MessagingException
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.service.SftpService
import uk.gov.dluhc.printapi.service.StatisticsUpdateService
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse

@ExtendWith(MockitoExtension::class)
internal class PrintResponseFileServiceTest {
    @Mock
    private lateinit var sftpService: SftpService

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var printResponseProcessingService: PrintResponseProcessingService

    @Mock
    private lateinit var statisticsUpdateService: StatisticsUpdateService

    @InjectMocks
    private lateinit var printResponseFileService: PrintResponseFileService

    @Test
    fun `should process print response file`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"
        val fileContent = "{\"BatchResponses\": [], \"PrintResponses\": []}"

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)
        val expectedPrintResponses =
            PrintResponses.PrintResponsesBuilder()
                .withBatchResponses(emptyList())
                .withPrintResponses(emptyList<PrintResponse>())
                .build()
        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(expectedPrintResponses)
        given(printResponseProcessingService.processBatchResponses(any())).willReturn(emptyList())

        // When
        printResponseFileService.processPrintResponseFile(directory, fileName)

        // Then
        val inOrder = inOrder(sftpService, objectMapper, printResponseProcessingService)
        inOrder.verify(sftpService).fetchFileFromOutBoundDirectory(directory, fileName)
        inOrder.verify(objectMapper).readValue(fileContent, PrintResponses::class.java)
        inOrder.verify(printResponseProcessingService).processBatchResponses(expectedPrintResponses.batchResponses)
        inOrder.verify(printResponseProcessingService).processPrintResponses(expectedPrintResponses.printResponses)
        inOrder.verify(sftpService).removeFileFromOutBoundDirectory(directory, fileName)
    }

    @Test
    fun `should process print response file with batch responses and send statistics updates`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"

        // Since we are mocking the object mapper, an attempt at the json content here would be misleading
        // and would be likely to be incorrect.
        val fileContent = "TEST_JSON_CONTENT"

        val batchResponse = buildBatchResponse()
        val printResponses = PrintResponses.PrintResponsesBuilder()
            .withBatchResponses(listOf(batchResponse))
            .withPrintResponses(emptyList<PrintResponse>())
            .build()
        val firstCertificate = buildCertificate(batchId = batchResponse.batchId)
        val secondCertificate = buildCertificate(batchId = batchResponse.batchId)

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)

        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(printResponses)
        given(printResponseProcessingService.processBatchResponses(any()))
            .willReturn(listOf(firstCertificate, secondCertificate))

        // When
        printResponseFileService.processPrintResponseFile(directory, fileName)

        // Then
        val inOrder = inOrder(sftpService, objectMapper, printResponseProcessingService, statisticsUpdateService)
        inOrder.verify(sftpService).fetchFileFromOutBoundDirectory(directory, fileName)
        inOrder.verify(objectMapper).readValue(fileContent, PrintResponses::class.java)
        inOrder.verify(printResponseProcessingService).processBatchResponses(printResponses.batchResponses)
        inOrder.verify(printResponseProcessingService).processPrintResponses(printResponses.printResponses)
        inOrder.verify(sftpService).removeFileFromOutBoundDirectory(directory, fileName)
        inOrder.verify(statisticsUpdateService).triggerVoterCardStatisticsUpdate(firstCertificate.sourceReference!!)
        inOrder.verify(statisticsUpdateService).triggerVoterCardStatisticsUpdate(secondCertificate.sourceReference!!)
    }

    @Test
    fun `should log and not throw exception if file is not found when deleting`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"
        val fileContent = "{\"BatchResponses\": [], \"PrintResponses\": []}"

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)
        val expectedPrintResponses =
            PrintResponses.PrintResponsesBuilder()
                .withBatchResponses(emptyList())
                .withPrintResponses(emptyList<PrintResponse>())
                .build()
        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(expectedPrintResponses)
        given(printResponseProcessingService.processBatchResponses(any())).willReturn(emptyList())

        // Returns false when file not found
        given(sftpService.removeFileFromOutBoundDirectory(any(), any())).willReturn(false)
        TestLogAppender.reset()

        // When
        printResponseFileService.processPrintResponseFile(directory, fileName)

        // Then
        val inOrder = inOrder(sftpService, objectMapper, printResponseProcessingService)
        inOrder.verify(sftpService).fetchFileFromOutBoundDirectory(directory, fileName)
        inOrder.verify(objectMapper).readValue(fileContent, PrintResponses::class.java)
        inOrder.verify(printResponseProcessingService).processBatchResponses(expectedPrintResponses.batchResponses)
        inOrder.verify(printResponseProcessingService).processPrintResponses(expectedPrintResponses.printResponses)
        inOrder.verify(sftpService).removeFileFromOutBoundDirectory(directory, fileName)
        assertThat(
            TestLogAppender.hasLog(
                "File $fileName was not found when trying to remove from the directory $directory",
                Level.WARN
            )
        ).isTrue
    }

    @Test
    fun `should log and not throw exception given existing file and an io error when deleting the file`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"
        val fileContent = "{\"BatchResponses\": [], \"PrintResponses\": []}"

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)
        val expectedPrintResponses =
            PrintResponses.PrintResponsesBuilder()
                .withBatchResponses(emptyList())
                .withPrintResponses(emptyList<PrintResponse>())
                .build()
        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(expectedPrintResponses)
        given(printResponseProcessingService.processBatchResponses(any())).willReturn(emptyList())
        val exception = MessagingException("Some error occurred")
        given(sftpService.removeFileFromOutBoundDirectory(any(), any())).willThrow(exception)
        TestLogAppender.reset()

        // When

        printResponseFileService.processPrintResponseFile(directory, fileName)

        // Then
        val inOrder = inOrder(sftpService, objectMapper, printResponseProcessingService)
        inOrder.verify(sftpService).fetchFileFromOutBoundDirectory(directory, fileName)
        inOrder.verify(objectMapper).readValue(fileContent, PrintResponses::class.java)
        inOrder.verify(printResponseProcessingService).processBatchResponses(expectedPrintResponses.batchResponses)
        inOrder.verify(printResponseProcessingService).processPrintResponses(expectedPrintResponses.printResponses)
        inOrder.verify(sftpService).removeFileFromOutBoundDirectory(directory, fileName)
        assertThat(
            TestLogAppender.hasLogMatchingRegex(
                "An error occurred when trying to remove file $fileName from the directory $directory. ${exception.message}",
                Level.ERROR
            )
        ).isTrue
    }
}
