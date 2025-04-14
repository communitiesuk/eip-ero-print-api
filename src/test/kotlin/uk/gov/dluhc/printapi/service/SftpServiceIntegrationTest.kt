package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.io.IOException

internal class SftpServiceIntegrationTest : IntegrationTest() {

    @Nested
    inner class FetchRemotePrintResponseFile {

        @Test
        fun `should fetch content of the remote file and return as a string`() {
            // Given
            val filenameToProcess = "status-20220928235441999.json"
            val printResponses = buildPrintResponses()
            val expectedResponseString = objectMapper.writeValueAsString(printResponses)

            writeContentToRemoteOutBoundDirectory(filenameToProcess, expectedResponseString)

            // When
            val actualPrintResponsesString =
                sftpService.fetchFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // Then
            assertThat(actualPrintResponsesString).isEqualTo(expectedResponseString)
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isTrue
        }

        @Test
        fun `should throw exception given missing remote file`() {
            // Given
            val filenameToProcess = "missing-file.json"

            // When
            val ex = catchThrowableOfType(IOException::class.java) {
                sftpService.fetchFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)
            }

            // Then
            assertThat(ex).hasMessageContaining("Failed to read file [EROP/Dev/OutBound/missing-file.json]")
        }
    }

    @Nested
    inner class RemoveRemotePrintResponseFile {
        @Test
        fun `should remove remote file`() {
            // Given
            val filenameToProcess = "status-20220928235441999.json"
            val printResponses = buildPrintResponses()
            val expectedResponseString = objectMapper.writeValueAsString(printResponses)

            writeContentToRemoteOutBoundDirectory(filenameToProcess, expectedResponseString)

            // When
            val actualRemovedResponses =
                sftpService.removeFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // Then
            assertThat(actualRemovedResponses).isTrue
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
        }

        @Test
        fun `should return false given missing remote file`() {
            // Given
            val filenameToProcess = "missing-file.json"

            // When
            val result = sftpService.removeFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // Then
            assertThat(result).isFalse()
        }
    }
}
