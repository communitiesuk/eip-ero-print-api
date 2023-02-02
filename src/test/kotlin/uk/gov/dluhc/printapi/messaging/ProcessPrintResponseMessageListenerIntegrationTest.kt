package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseMessage
import uk.gov.dluhc.printapi.printprovider.models.PrintResponse
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildPrintResponse
import java.time.Instant
import java.util.concurrent.TimeUnit

internal class ProcessPrintResponseMessageListenerIntegrationTest : IntegrationTest() {
    @Test
    fun `should process print response message`() {
        assert(true)
        // Given
        val requestId = aValidRequestId()
        val batchId = aValidBatchId()
        val certificate = buildCertificate(
            status = Status.ASSIGNED_TO_BATCH,
            printRequests = mutableListOf(
                buildPrintRequest(
                    batchId = batchId,
                    requestId = requestId,
                    printRequestStatuses = listOf(
                        buildPrintRequestStatus(
                            status = Status.ASSIGNED_TO_BATCH,
                            eventDateTime = Instant.now().minusSeconds(10)
                        )
                    )
                )
            )
        )
        certificateRepository.save(certificate)

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
            val saved = certificateRepository.getByPrintRequestsRequestId(printResponse.requestId)
            assertThat(saved).isNotNull
            assertThat(saved!!.status).isEqualTo(Status.IN_PRODUCTION)
        }
    }
}
