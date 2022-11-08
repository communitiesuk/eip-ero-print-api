package uk.gov.dluhc.printapi.jobs

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponsesBatchJobIntegrationTest : IntegrationTest() {

    @Test
    fun `should process outbound directory for print responses`() {
        // Given
        val validFile1 = "status-20221101171156056.json"
        val validFile2 = "status-20221101171156057.json"
        val validFile3 = "status-20220928235441000.json"
        val unknownFileTypeFileName = "status-unknown-file.json"

        writeFileToRemoteOutBoundDirectory(validFile1)
        writeFileToRemoteOutBoundDirectory(validFile2)
        writeContentToRemoteOutBoundDirectory(validFile3, objectMapper.writeValueAsString(buildPrintResponses()))
        writeContentToRemoteOutBoundDirectory(unknownFileTypeFileName, "This is an unknown file type")

        val originalFileList = listOf(validFile1, validFile2, validFile3, unknownFileTypeFileName)
        val renamedToProcessingList = listOf("$validFile1.processing", "$validFile2.processing", "$validFile3.processing")

        val totalFilesOnSftpServerBeforeProcessing = getSftpOutboundDirectoryFileNames()
        // Files are present on the server before processing
        assertThat(totalFilesOnSftpServerBeforeProcessing)
            .hasSize(4)
            .containsAll(originalFileList)

        // When
        processPrintResponsesBatchJob.pollAndProcessPrintResponses()

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(renamedToProcessingList)).isFalse
            assertThat(hasFilesPresentInOutboundDirectory(listOf(unknownFileTypeFileName))).isTrue
            assertThat(getSftpOutboundDirectoryFileNames()).hasSize(1)
        }
    }
}
