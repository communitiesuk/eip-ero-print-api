package uk.gov.dluhc.printapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
internal class PrintResponseFileServiceTest {
    @Mock
    private lateinit var sftpService: SftpService

    @InjectMocks
    private lateinit var printResponseFileService: PrintResponseFileService

    @Test
    fun `should process print response file`() {
        // Given
        val directory = "EROP/Dev/OutBound"
        val fileName = "status-20221101171156056.json"
        val fileContent = "{\"BatchResponses\": [], \"PrintResponses\": []}"

        given(sftpService.fetchFileFromOutBoundDirectory(any(), any())).willReturn(fileContent)

        // When
        printResponseFileService.processPrintResponseFile(directory, fileName)

        // Then
        verify(sftpService).fetchFileFromOutBoundDirectory(directory, fileName)
        verify(sftpService).removeFileFromOutBoundDirectory(directory, fileName)
    }
}
