package uk.gov.dluhc.printapi.messaging

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

internal class ProcessPrintResponseFileMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should fetch remote print response file and delete it`() {
        // Given
        val filenameToProcess = "status-20220928235441999.json"
        val expectedPrintResponses = buildPrintResponses()

        writePrintResponsesToSftpOutboundDirectory(filenameToProcess, expectedPrintResponses)

        val message = ProcessPrintResponseFileMessage(
            directory = PRINT_RESPONSE_DOWNLOAD_PATH,
            fileName = filenameToProcess,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        val stopWatch = StopWatch.createStarted()
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            assertThat(fileFoundInOutboundDirectory(filenameToProcess)).isFalse

            stopWatch.stop()
            logger.info("completed assertions in $stopWatch")
        }

        // todo assert db updates after completing service implementation
    }
}
