package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseFileMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should fetch remote print response file and delete it`() {
        // Given
        val filenameToProcess = "status-20220928235441999.json"
        val printResponses = buildPrintResponses()
        val printResponsesAsString = objectMapper.writeValueAsString(printResponses)

        writeContentToRemoteOutBoundDirectory(filenameToProcess, printResponsesAsString)

        val message = ProcessPrintResponseFileMessage(
            directory = PRINT_RESPONSE_DOWNLOAD_PATH,
            fileName = filenameToProcess,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        await.atMost(3, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
        }

        // todo assert db updates after completing service implementation
    }
}
