package uk.gov.dluhc.printapi.messaging.service

import ch.qos.logback.classic.Level
import com.fasterxml.jackson.databind.ObjectMapper
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
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
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.service.SftpService
import uk.gov.dluhc.printapi.testsupport.TestLogAppender
import java.io.IOException

@ExtendWith(MockitoExtension::class)
internal class PrintResponseFileServiceTest {
    @Mock
    private lateinit var sftpService: SftpService

    @Mock
    private lateinit var objectMapper: ObjectMapper

    @Mock
    private lateinit var printResponseProcessingService: PrintResponseProcessingService

    @InjectMocks
    private lateinit var printResponseFileService: PrintResponseFileService

    @Test
    fun `should process print response file`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"
        val fileContent = "{\"BatchResponses\": [], \"PrintResponses\": []}"

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)
        val expectedPrintResponses = PrintResponses().withBatchResponses(emptyList()).withPrintResponses(emptyList())
        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(expectedPrintResponses)

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
    fun `should log and not throw exception if file is not found when deleting`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"
        val fileContent = "{\"BatchResponses\": [], \"PrintResponses\": []}"

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)
        val expectedPrintResponses = PrintResponses().withBatchResponses(emptyList()).withPrintResponses(emptyList())
        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(expectedPrintResponses)
        val messagingException = MessagingException(
            "Failed to remove file",
            IOException(SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "No such file"))
        )
        val exception = IOException(messagingException)
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
        val expectedPrintResponses = PrintResponses().withBatchResponses(emptyList()).withPrintResponses(emptyList())
        given(objectMapper.readValue(fileContent, PrintResponses::class.java))
            .willReturn(expectedPrintResponses)
        val exception = IOException(MessagingException("Some error occurred"))
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
                "An error occurred when trying to remove file $fileName from the directory $directory. ${exception.cause!!.message}",
                Level.ERROR
            )
        ).isTrue
    }
}
