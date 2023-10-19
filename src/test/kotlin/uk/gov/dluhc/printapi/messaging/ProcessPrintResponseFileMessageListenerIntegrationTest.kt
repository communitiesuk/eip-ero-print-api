package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseFileMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should fetch remote print response file and delete it`() {
        // Given
        val filenameToProcess = "status-20220928235441999.json"
        val printResponses = buildPrintResponses()
        val printResponsesAsString = objectMapper.writeValueAsString(printResponses)

        val certificates = printResponses.batchResponses.map {
            buildCertificate(
                status = PrintRequestStatus.Status.SENT_TO_PRINT_PROVIDER,
                batchId = it.batchId
            )
        }
        certificateRepository.saveAll(certificates)

        writeContentToRemoteOutBoundDirectory(filenameToProcess, printResponsesAsString)

        val message = ProcessPrintResponseFileMessage(
            directory = PRINT_RESPONSE_DOWNLOAD_PATH,
            fileName = filenameToProcess,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
            certificates.forEach { assertUpdateStatisticsMessageSent(it.sourceReference!!) }
        }

        // todo assert db updates after completing service implementation
    }
}
