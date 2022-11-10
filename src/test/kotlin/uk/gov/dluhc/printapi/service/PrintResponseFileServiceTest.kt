package uk.gov.dluhc.printapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.verify
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses

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
        inOrder.verify(printResponseProcessingService).processBatchAndPrintResponses(expectedPrintResponses)
        inOrder.verify(sftpService).removeFileFromOutBoundDirectory(directory, fileName)
    }
}
