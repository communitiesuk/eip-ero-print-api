package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.messaging.models.SourceType.ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildPrintRequest
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildTemporaryCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.messaging.model.buildApplicationRemovedMessage
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.JULY
import java.time.Month.JUNE
import java.time.Month.MARCH
import java.time.Month.MAY
import java.util.concurrent.TimeUnit

internal class ApplicationRemovedMessageListenerTest : IntegrationTest() {

    @Test
    fun `should process application removed message and set initial and final removal dates on certificate`() {
        // Given
        val certificate = buildCertificate(
            issueDate = LocalDate.of(2023, APRIL, 1), // to include 3 bank holidays in calculation
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
        // currently 28 working days following issue date - refer to application.yml
        val expectedInitialRemovalDate = LocalDate.of(2023, MAY, 16)
        val expectedFinalRemovalDate = LocalDate.of(2032, JULY, 1)

        // When
        sqsTemplate.send(applicationRemovedQueueName, payload)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val response = certificateRepository.findAll()
            assertThat(response).hasSize(1)
            val saved = response[0]
            assertThat(saved).hasInitialRetentionRemovalDate(expectedInitialRemovalDate)
            assertThat(saved).hasFinalRetentionRemovalDate(expectedFinalRemovalDate)
        }
    }

    @Test
    fun `should process application removed message and set final removal date on temporary certificate`() {
        // Given
        val temporaryCertificate = buildTemporaryCertificate(
            issueDate = LocalDate.of(2023, APRIL, 1)
        )
        temporaryCertificateRepository.save(temporaryCertificate)
        val payload = buildApplicationRemovedMessage(
            sourceReference = temporaryCertificate.sourceReference!!,
            gssCode = temporaryCertificate.gssCode!!
        )
        val expectedFinalRemovalDate = LocalDate.of(2024, JULY, 1)

        // When
        sqsTemplate.send(applicationRemovedQueueName, payload)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val response = temporaryCertificateRepository.findAll()
            assertThat(response).hasSize(1)
            val saved = response[0]
            assertThat(saved).hasFinalRetentionRemovalDate(expectedFinalRemovalDate)
        }
    }

    @Test
    fun `should process application removed message and set initial and final removal date on anonymous elector document`() {
        // Given
        val anonymousElectorDocument = buildAnonymousElectorDocument(
            issueDate = LocalDate.of(2023, MARCH, 10)
        )
        anonymousElectorDocumentRepository.save(anonymousElectorDocument)
        val payload = buildApplicationRemovedMessage(
            sourceType = ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT,
            sourceReference = anonymousElectorDocument.sourceReference,
            gssCode = anonymousElectorDocument.gssCode
        )
        val expectedInitialRemovalDate = LocalDate.of(2024, JUNE, 10)
        val expectedFinalRemovalDate = LocalDate.of(2032, JULY, 1)

        // When
        sqsTemplate.send(applicationRemovedQueueName, payload)

        // Then
        await.atMost(5, TimeUnit.SECONDS).untilAsserted {
            val response = anonymousElectorDocumentRepository.findAll()
            assertThat(response).hasSize(1)
            val saved = response[0]
            assertThat(saved).hasInitialRetentionRemovalDate(expectedInitialRemovalDate)
            assertThat(saved).hasFinalRetentionRemovalDate(expectedFinalRemovalDate)
        }
    }
}
