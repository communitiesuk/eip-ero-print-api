package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.dluhc.printapi.config.SftpProperties
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage

@ExtendWith(MockitoExtension::class)
internal class PrintResponseFileReadinessServiceTest {

    @Mock
    private lateinit var sftpService: SftpService

    @Mock
    private lateinit var sftpProperties: SftpProperties

    @Mock
    private lateinit var printMessagingService: PrintMessagingService

    @InjectMocks
    private lateinit var printResponseFileReadinessService: PrintResponseFileReadinessService

    companion object {
        const val DIRECTORY = "/some-directory"
    }

    @Test
    fun `should not mark print response file and not send to queue when no files are present`() {
        // Given

        given(sftpProperties.printResponseDownloadDirectory).willReturn(DIRECTORY)
        given(sftpService.identifyFilesToBeProcessed(any())).willReturn(emptyList())

        // When
        printResponseFileReadinessService.markPrintResponseFileForProcessing()

        // Then
        verify(sftpService).identifyFilesToBeProcessed(DIRECTORY)
        verifyNoInteractions(printMessagingService)
        verifyNoMoreInteractions(sftpService)
    }

    @Test
    fun `should mark print response file and send to queue when files are present`() {
        // Given
        val matchingFileName1 = "status-20221101171156056.json"
        val matchingFileName2 = "status-20221101171156057.json"
        val renamedFileName1 = "$matchingFileName1.processing"
        val renamedFileName2 = "$matchingFileName2.processing"

        given(sftpProperties.printResponseDownloadDirectory).willReturn(DIRECTORY)
        given(sftpService.identifyFilesToBeProcessed(any())).willReturn(listOf(matchingFileName1, matchingFileName2))
        given(sftpService.markFileForProcessing(eq(DIRECTORY), eq(matchingFileName1))).willReturn(renamedFileName1)
        given(sftpService.markFileForProcessing(eq(DIRECTORY), eq(matchingFileName2))).willReturn(renamedFileName2)

        // When
        printResponseFileReadinessService.markPrintResponseFileForProcessing()

        // Then
        verify(sftpService).identifyFilesToBeProcessed(DIRECTORY)
        verify(sftpService).markFileForProcessing(DIRECTORY, matchingFileName1)
        verify(sftpService).markFileForProcessing(DIRECTORY, matchingFileName2)
        verify(printMessagingService).submitPrintResponseFileForProcessing(
            ProcessPrintResponseFileMessage(DIRECTORY, renamedFileName1)
        )
        verify(printMessagingService).submitPrintResponseFileForProcessing(
            ProcessPrintResponseFileMessage(DIRECTORY, renamedFileName2)
        )
        verifyNoMoreInteractions(sftpService, printMessagingService)
    }

    @Test
    fun `should continue marking print response file and sending to queue when one of the file marking fails due to exception`() {
        // Given
        val matchingFileName1 = "status-20221101171156056.json"
        val matchingFileThatThrowsException = "status-20221101171156057.json"
        val matchingFileName3 = "status-20221101171156058.json"
        val renamedFileName1 = "$matchingFileName1.processing"
        val renamedFileName3 = "$matchingFileName3.processing"
        val expectedExceptionThrown = RuntimeException("Some error")

        given(sftpProperties.printResponseDownloadDirectory).willReturn(DIRECTORY)
        given(sftpService.identifyFilesToBeProcessed(any())).willReturn(listOf(matchingFileName1, matchingFileThatThrowsException, matchingFileName3))
        given(sftpService.markFileForProcessing(eq(DIRECTORY), eq(matchingFileName1))).willReturn(renamedFileName1)
        given(sftpService.markFileForProcessing(eq(DIRECTORY), eq(matchingFileThatThrowsException))).willThrow(expectedExceptionThrown)
        given(sftpService.markFileForProcessing(eq(DIRECTORY), eq(matchingFileName3))).willReturn(renamedFileName3)

        // When
        printResponseFileReadinessService.markPrintResponseFileForProcessing()

        // Then
        verify(sftpService).identifyFilesToBeProcessed(DIRECTORY)
        verify(sftpService).markFileForProcessing(DIRECTORY, matchingFileName1)
        verify(sftpService).markFileForProcessing(DIRECTORY, matchingFileThatThrowsException)
        verify(sftpService).markFileForProcessing(DIRECTORY, matchingFileName3)
        verify(printMessagingService).submitPrintResponseFileForProcessing(
            ProcessPrintResponseFileMessage(DIRECTORY, renamedFileName1)
        )
        verify(printMessagingService).submitPrintResponseFileForProcessing(
            ProcessPrintResponseFileMessage(DIRECTORY, renamedFileName3)
        )
        verifyNoMoreInteractions(sftpService, printMessagingService)
    }
}
