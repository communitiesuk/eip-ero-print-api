package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.messaging.MessagingException
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
            val actualPrintResponsesString = sftpService.fetchFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // Then
            assertThat(actualPrintResponsesString).isEqualTo(expectedResponseString)
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isTrue
        }

        @Test
        fun `should throw exception given missing remote file`() {
            // Given
            val filenameToProcess = "missing-file.json"

            // When
            val ex =
                Assertions.catchThrowableOfType(
                    { sftpService.fetchFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess) },
                    MessagingException::class.java
                )

            // Then
            assertThat(ex).hasMessageContaining("failed to read file EROP/Dev/OutBound/missing-file.json")
            assertThat(ex).hasCauseInstanceOf(IOException::class.java)
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
            val actualRemovedResponses = sftpService.removeFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // Then
            assertThat(actualRemovedResponses).isTrue
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
        }

        @Test
        fun `should throw exception given missing remote file`() {
            // Given
            val filenameToProcess = "missing-file.json"

            // When
            val ex =
                Assertions.catchThrowableOfType(
                    { sftpService.removeFileFromOutBoundDirectory(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess) },
                    MessagingException::class.java
                )

            // Then
            assertThat(ex).hasMessageContaining("Failed to execute on session; nested exception is java.io.IOException: Failed to remove file")
            assertThat(ex).hasCauseInstanceOf(IOException::class.java)
        }
    }
}
