package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildApplicationRemovedMessage
import java.time.LocalDate
import java.util.concurrent.TimeUnit

internal class ApplicationRemovedMessageListenerTest : IntegrationTest() {

    @Test
    fun `should process message and set delivery info removal date`() {
        // Given
        val certificate = buildCertificate(
            issueDate = LocalDate.of(2023, 4, 1), // to include 3 bank holidays in calculation
            printRequests = listOf(
                buildPrintRequest(delivery = buildDelivery()),
                buildPrintRequest(delivery = buildDelivery())
            )
        )
        certificateRepository.save(certificate)
        val payload = buildApplicationRemovedMessage(
            sourceReference = certificate.sourceReference!!,
            gssCode = certificate.gssCode!!
        )
        val expectedInitialRemovalDate = LocalDate.of(2023, 5, 16)

        // When
        sqsMessagingTemplate.convertAndSend(applicationRemovedQueueName, payload)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val response = certificateRepository.findAll()
            assertThat(response).hasSize(1)
            val saved = response[0]
            assertThat(saved.initialRetentionRemovalDate).isEqualTo(expectedInitialRemovalDate)
        }
    }
}
