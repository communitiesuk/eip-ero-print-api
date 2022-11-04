package uk.gov.dluhc.printapi.service

import com.fasterxml.jackson.core.JsonParseException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessagingException
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.jobs.ProcessPrintResponsesBatchJobIntegrationTest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildBatchResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.io.File
import java.io.IOException

internal class PrintResponseFileServiceIntegrationTest : IntegrationTest() {

    @Nested
    inner class FetchRemotePrintResponseFile {

        @ParameterizedTest
        @CsvSource(
            value = [
                "0, 0",
                "0, 1",
                "1, 0",
                "1, 1"
            ]
        )
        fun `should fetch remote file and unmarshall to a PrintResponses object`(
            noOfBatchElements: Int,
            noOfPrintElements: Int
        ) {
            // Given
            val filenameToProcess = "status-20220928235441999.json"
            val expectedPrintResponses = buildPrintResponses(
                batchResponses = (0 until noOfBatchElements).map { buildBatchResponse() },
                printResponses = (0 until noOfPrintElements).map { buildPrintResponse() },
            )

            writePrintResponsesFileToSftpOutboundDirectory(filenameToProcess, expectedPrintResponses)
            val filePathToProcess = FilenameFactory.createFileNamePath(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // When
            val actualPrintResponses = printResponseFileService.fetchAndUnmarshallPrintResponses(filePathToProcess)

            // Then
            assertThat(actualPrintResponses).isEqualTo(expectedPrintResponses)
            assertThat(fileFoundInOutboundDirectory(filenameToProcess)).isTrue
        }

        @Test
        fun `should throw exception given invalid JSON file`() {
            // Given
            val filenameToProcess = "invalid-json.json"
            sftpOutboundTemplate.send(
                MessageBuilder
                    .withPayload(File(ProcessPrintResponsesBatchJobIntegrationTest.LOCAL_SFTP_TEST_DIRECTORY, filenameToProcess))
                    .build()
            )

            val filePathToProcess = FilenameFactory.createFileNamePath(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // When
            val ex =
                Assertions.catchThrowableOfType(
                    { printResponseFileService.fetchAndUnmarshallPrintResponses(filePathToProcess) },
                    MessagingException::class.java
                )

            // Then
            assertThat(ex).hasMessageContaining("Unexpected character (' ' (code 32)) in numeric value")
            assertThat(ex).hasCauseInstanceOf(JsonParseException::class.java)
        }

        @Test
        fun `should throw exception given missing remote file`() {
            // Given
            val filenameToProcess = "missing-file.json"
            val filePathToProcess = FilenameFactory.createFileNamePath(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // When
            val ex =
                Assertions.catchThrowableOfType(
                    { printResponseFileService.fetchAndUnmarshallPrintResponses(filePathToProcess) },
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
            val expectedPrintResponses = buildPrintResponses()

            writePrintResponsesFileToSftpOutboundDirectory(filenameToProcess, expectedPrintResponses)
            val filePathToProcess = "$PRINT_RESPONSE_DOWNLOAD_PATH/$filenameToProcess"

            // When
            val actualRemovedResponses = printResponseFileService.removeRemoteFile(filePathToProcess)

            // Then
            assertThat(actualRemovedResponses).isTrue
            assertThat(fileFoundInOutboundDirectory(filenameToProcess)).isFalse
        }

        @Test
        fun `should throw exception given missing remote file`() {
            // Given
            val filenameToProcess = "missing-file.json"
            val filePathToProcess = FilenameFactory.createFileNamePath(PRINT_RESPONSE_DOWNLOAD_PATH, filenameToProcess)

            // When
            val ex =
                Assertions.catchThrowableOfType(
                    { printResponseFileService.removeRemoteFile(filePathToProcess) },
                    MessagingException::class.java
                )

            // Then
            assertThat(ex).hasMessageContaining("Failed to execute on session; nested exception is java.io.IOException: Failed to remove file")
            assertThat(ex).hasCauseInstanceOf(IOException::class.java)
        }
    }
}
