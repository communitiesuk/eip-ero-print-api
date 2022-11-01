package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses

internal class PrintResponseFileServiceIntegrationTest : IntegrationTest() {

    @Nested
    inner class FetchRemotePrintResponseFile {
        @Test
        fun `should fetch remote file and unmarshall to a PrintResponses object`() {
            // Given
            val filenameToProcess = "status-20220928235441999.json"
            val expectedPrintResponses = buildPrintResponses()

            writePrintResponsesFileToSftpOutboundDirectory(filenameToProcess, expectedPrintResponses)
            val filePathToProcess = "$PRINT_RESPONSE_DOWNLOAD_PATH/$filenameToProcess"

            // When
            val actualPrintResponses = printResponseFileService.fetchAndUnmarshallPrintResponses(filePathToProcess)

            // Then
            assertThat(actualPrintResponses).isEqualTo(expectedPrintResponses)
            assertThat(fileFoundInOutboundDirectory(filenameToProcess)).isTrue
        }
    }

    @Nested
    inner class RemoveRemotePrintResponseFile {
        @Test
        fun `should remove remote file`() {
            // Given
            val filenameToProcess = "status-20220928235441999.json"
            val expectedPrintResponses = buildPrintResponses()

            writePrintResponsesFileToSftpOutboundDirectory(filenameToProcess, expectedPrintResponses)
            val filePathToProcess = "$PRINT_RESPONSE_DOWNLOAD_PATH/$filenameToProcess"

            // When
            val actualRemovedResponses = printResponseFileService.removeRemoteFile(filePathToProcess)

            // Then
            assertThat(actualRemovedResponses).isTrue
            assertThat(fileFoundInOutboundDirectory(filenameToProcess)).isFalse
        }
    }
}
