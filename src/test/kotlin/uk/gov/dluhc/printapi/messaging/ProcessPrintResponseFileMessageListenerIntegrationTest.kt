package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.NullSource
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.config.SftpContainerConfiguration.Companion.PRINT_RESPONSE_DOWNLOAD_PATH
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponses
import java.util.concurrent.TimeUnit

@TestInstance(Lifecycle.PER_CLASS)
internal class ProcessPrintResponseFileMessageListenerIntegrationTest : IntegrationTest() {

    // @Test
    @ParameterizedTest
    @NullSource
    @CsvSource("true", "false")
    fun `should fetch remote print response file and send message to application api`(isFromApplicationsApi: Boolean?) {
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
            isFromApplicationsApi = isFromApplicationsApi,
        )

        // When
        processPrintResponseFileMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            assertThat(hasFilesPresentInOutboundDirectory(listOf(filenameToProcess))).isFalse
            certificates.forEach {
                if (isFromApplicationsApi == true) {
                    assertUpdateApplicationStatisticsMessageSent(it.sourceReference!!)
                } else {
                    assertUpdateStatisticsMessageSent(it.sourceReference!!)
                }
            }
        }
    }
}
