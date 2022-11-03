package uk.gov.dluhc.printapi.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.integration.support.MessageBuilder
import uk.gov.dluhc.printapi.config.IntegrationTest
import java.io.File

internal class ProcessPrintResponsesBatchJobIntegrationTest : IntegrationTest() {

    companion object {
        const val LOCAL_SFTP_TEST_DIRECTORY = "src/test/resources/sftp/local/OutBound"
    }

    @Test
    fun `should process outbound directory for print responses`() {
        // Given
        val fileName1 = "status-20221101171156056.json"
        val fileName2 = "status-20221101171156057.json"

        val expectedFileList = listOf("$fileName1.processing", "$fileName2.processing")
        val expectedFileCount = 2
        val originalFileNames = listOf(fileName1, fileName2)

        originalFileNames.forEach { fileName ->
            sftpOutboundTemplate.send(
                MessageBuilder
                    .withPayload(File("$LOCAL_SFTP_TEST_DIRECTORY/$fileName"))
                    .build()
            )
        }

        // When
        processPrintResponsesBatchJob.pollAndProcessPrintResponses()

        // Then
        val filesMarkedForProcessingOnSftpServer = getSftpOutboundDirectoryFileNames()
        assertThat(filesMarkedForProcessingOnSftpServer.size).isEqualTo(expectedFileCount)
        assertThat(filesMarkedForProcessingOnSftpServer)
            .hasSize(expectedFileCount)
            .doesNotContainAnyElementsOf(originalFileNames)
            .containsExactlyInAnyOrderElementsOf(expectedFileList)
        // TODO EIP1-2261 will assert that SQS message is sent to print-api queue
    }
}
