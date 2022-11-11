package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseMessageListenerIntegrationTest : IntegrationTest() {
    @Test
    fun `should process print response message`() {
        assert(true)
        // Given
        val requestId = aValidRequestId()
        val printDetailsId = UUID.randomUUID()
        val batchId = aValidBatchId()

        val details = buildPrintDetails(
            id = printDetailsId,
            batchId = batchId,
            printRequestStatuses = mutableListOf(
                PrintRequestStatus(Status.ASSIGNED_TO_BATCH, OffsetDateTime.now(clock))
            ),
            requestId = requestId,
            eroWelsh = buildElectoralRegistrationOffice()
        )
        printDetailsRepository.save(details)

        val printResponse = buildPrintResponse(
            requestId = requestId,
            status = PrintResponse.Status.SUCCESS,
            statusStep = PrintResponse.StatusStep.IN_PRODUCTION
        )

        val message = ProcessPrintResponseMessage(
            requestId = requestId,
            timestamp = printResponse.timestamp,
            status = ProcessPrintResponseMessage.Status.SUCCESS,
            statusStep = ProcessPrintResponseMessage.StatusStep.IN_MINUS_PRODUCTION,
            message = printResponse.message,
        )

        // When
        processPrintResponseMessageQueue.submit(message)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val saved = printDetailsRepository.getByRequestId(printResponse.requestId)
            assertThat(saved.status).isEqualTo(Status.IN_PRODUCTION)
        }
    }
}
