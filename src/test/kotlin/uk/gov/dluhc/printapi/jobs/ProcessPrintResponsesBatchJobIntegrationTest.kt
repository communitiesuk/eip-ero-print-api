package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import org.springframework.integration.support.MessageBuilder
import uk.gov.dluhc.printapi.config.IntegrationTest
import java.io.File
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }

internal class ProcessPrintResponsesBatchJobIntegrationTest : IntegrationTest() {

    companion object {
        const val LOCAL_SFTP_TEST_DIRECTORY = "src/test/resources/sftp/local/OutBound"
    }

    @Test
    fun `should process outbound directory for print responses`() {
        // Given
        val fileName1 = "status-20221101171156056.json"
        val fileName2 = "status-20221101171156057.json"
        val originalFileList = listOf(fileName1, fileName2)
        val expectedFileList = listOf("$fileName1.processing", "$fileName2.processing")

        originalFileList.forEach { fileName ->
            sftpOutboundTemplate.send(
                MessageBuilder
                    .withPayload(File("$LOCAL_SFTP_TEST_DIRECTORY/$fileName"))
                    .build()
            )
        }

        val printResponseFileCountOnSftpServerBeforeProcessing = hasFilesPresentInOutboundDirectory(originalFileList)

        // When
        processPrintResponsesBatchJob.pollAndProcessPrintResponses()

        // Then
        assertThat(printResponseFileCountOnSftpServerBeforeProcessing).isTrue

        val stopWatch = StopWatch.createStarted()
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(expectedFileList)).isFalse
            assertThat(getSftpOutboundDirectoryFileNames()).isEmpty()
            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }
    }
}
