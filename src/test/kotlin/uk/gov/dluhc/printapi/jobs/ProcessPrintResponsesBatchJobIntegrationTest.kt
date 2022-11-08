package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger { }

internal class ProcessPrintResponsesBatchJobIntegrationTest : IntegrationTest() {

    @Test
    fun `should process outbound directory for print responses`() {
        // Given
        val fileName1 = "status-20221101171156056.json"
        val fileName2 = "status-20221101171156057.json"
        val fileName3 = "status-20220928235441000.json"

        writeFileToRemoteOutBoundDirectory(fileName1)
        writeFileToRemoteOutBoundDirectory(fileName2)
        writeContentToRemoteOutBoundDirectory(fileName3, objectMapper.writeValueAsString(buildPrintResponses()))

        val originalFileList = listOf(fileName1, fileName2, fileName3)
        val filesRenamedToProcessingList = originalFileList.map { "$it.processing" }

        val printResponseFileCountOnSftpServerBeforeProcessing = hasFilesPresentInOutboundDirectory(originalFileList)

        // When
        processPrintResponsesBatchJob.pollAndProcessPrintResponses()

        // Then
        assertThat(printResponseFileCountOnSftpServerBeforeProcessing).isTrue // Files were present on the server before

        val stopWatch = StopWatch.createStarted()
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(filesRenamedToProcessingList)).isFalse
            assertThat(getSftpOutboundDirectoryFileNames()).isEmpty()
            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }
    }
}
