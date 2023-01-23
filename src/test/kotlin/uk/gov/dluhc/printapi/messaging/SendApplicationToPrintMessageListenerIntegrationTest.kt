package uk.gov.dluhc.printapi.messaging

import ch.qos.logback.classic.Level
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat.EASY_MINUS_READ
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat.LARGE_MINUS_PRINT
import uk.gov.dluhc.printapi.testsupport.TestLogAppender.Companion.hasLog
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.Assertions.assertThat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildCertificate
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS
import uk.gov.dluhc.printapi.messaging.models.SourceType as SqsSourceType

internal class SendApplicationToPrintMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val ero = buildElectoralRegistrationOfficeResponse(
            localAuthorities = mutableListOf(
                buildLocalAuthorityResponse(),
                buildLocalAuthorityResponse(contactDetailsWelsh = buildContactDetails())
            )
        )
        val localAuthority = ero.localAuthorities[1]
        val gssCode = localAuthority.gssCode
        val payload =
            buildSendApplicationToPrintMessage(gssCode = gssCode, supportingInformationFormat = EASY_MINUS_READ)
        wireMockService.stubEroManagementGetEroByGssCode(ero, gssCode)

        val expected = with(payload) {
            val certificate = Certificate(
                id = UUID.randomUUID(),
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                sourceType = SourceType.VOTER_CARD,
                vacNumber = aValidVacNumber(),
                applicationReceivedDateTime = applicationReceivedDateTime.toInstant(),
                gssCode = gssCode,
                issuingAuthority = localAuthority.name,
                issueDate = LocalDate.now(),
            )
            val printRequest = toPrintRequest(localAuthority, SupportingInformationFormat.EASY_READ)
            certificate.addPrintRequest(printRequest)
        }

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.atMost(5, SECONDS).untilAsserted {
            wireMockService.verifyEroManagementGetEro(gssCode)
            val response = certificateRepository.findAll()
            assertThat(response).hasSize(1)
            val saved = response[0]
            assertSavedCertificate(saved, expected)
        }
    }

    @Test
    fun `should process message received on queue given certificate exists`() {
        // Given
        val ero = buildElectoralRegistrationOfficeResponse(
            localAuthorities = mutableListOf(
                buildLocalAuthorityResponse(),
                buildLocalAuthorityResponse(contactDetailsWelsh = buildContactDetails())
            )
        )
        val localAuthority = ero.localAuthorities[1]
        val gssCode = localAuthority.gssCode
        val certificate = buildCertificate(gssCode = gssCode, sourceType = SourceType.VOTER_CARD)
        val expected = certificateRepository.save(certificate)

        // Resend must reference the same application for original saved certificate to be updated
        val payload = buildSendApplicationToPrintMessage(
            gssCode = gssCode,
            sourceType = SqsSourceType.VOTER_MINUS_CARD,
            sourceReference = certificate.sourceReference!!,
            supportingInformationFormat = LARGE_MINUS_PRINT,
            requestDateTime = expected.printRequests[0].requestDateTime!!.plusSeconds(5).atOffset(UTC)
        )
        wireMockService.stubEroManagementGetEroByGssCode(ero, gssCode)

        // add resend print request from processing new send to print request to expected certificate
        with(payload) {
            expected.addPrintRequest(
                toPrintRequest(
                    localAuthority,
                    SupportingInformationFormat.LARGE_PRINT
                )
            )
        }

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.atMost(5, SECONDS).untilAsserted {
            wireMockService.verifyEroManagementGetEro(gssCode)
            val response = certificateRepository.findAll()
            assertThat(response).hasSize(1)
            val saved = response[0]
            assertThat(saved.printRequests).hasSize(2)
            assertSavedCertificate(saved, expected)
        }
    }

    @Test
    fun `should not process message that does conform to validation constraints`() {
        // Given
        val payload = buildSendApplicationToPrintMessage(gssCode = "ABC") // gssCode must be 9 characters

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.atMost(5, SECONDS).untilAsserted {
            assertThat(hasLog("An exception occurred while invoking the handler method", Level.ERROR)).isTrue()
        }
    }

    private fun SendApplicationToPrintMessage.toPrintRequest(
        localAuthority: LocalAuthorityResponse,
        supportingInfoFormat: SupportingInformationFormat
    ): PrintRequest {
        val printRequest = PrintRequest(
            requestId = aValidRequestId(),
            vacVersion = "A",
            requestDateTime = requestDateTime.toInstant(),
            firstName = firstName,
            middleNames = middleNames,
            surname = surname,
            certificateLanguage = CertificateLanguage.EN,
            supportingInformationFormat = supportingInfoFormat,
            photoLocationArn = photoLocation,
            delivery = with(delivery) {
                Delivery(
                    addressee = addressee,
                    address = with(address) {
                        Address(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    },
                    deliveryClass = DeliveryClass.STANDARD,
                    deliveryAddressType = DeliveryAddressType.REGISTERED,
                    addressFormat = AddressFormat.UK,
                )
            },
            eroEnglish = with(localAuthority) {
                ElectoralRegistrationOffice(
                    name = "Electoral Registration Officer",
                    phoneNumber = contactDetailsEnglish.phone,
                    emailAddress = contactDetailsEnglish.email,
                    website = contactDetailsEnglish.website,
                    address = with(contactDetailsEnglish.address) {
                        Address(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    }
                )
            },
            eroWelsh = with(localAuthority) {
                ElectoralRegistrationOffice(
                    name = "Swyddog Cofrestru Etholiadol",
                    phoneNumber = contactDetailsWelsh!!.phone,
                    emailAddress = contactDetailsWelsh!!.email,
                    website = contactDetailsWelsh!!.website,
                    address = with(contactDetailsWelsh!!.address) {
                        Address(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    }
                )
            },
            statusHistory = mutableListOf(
                PrintRequestStatus(
                    status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                    eventDateTime = Instant.now()
                )
            ),
            userId = userId
        )
        return printRequest
    }

    private fun assertSavedCertificate(
        saved: Certificate,
        expected: Certificate
    ) {
        assertThat(saved)
            .hasId()
            .hasVacNumber()
            .hasSourceType(expected.sourceType)
            .hasSourceReference(expected.sourceReference)
            .hasApplicationReference(expected.applicationReference)
            .hasApplicationReceivedDateTime(expected.applicationReceivedDateTime)
            .hasIssuingAuthority(expected.issuingAuthority)
            .hasIssueDate(expected.issueDate)
            .hasSuggestedExpiryDate(expected.suggestedExpiryDate)
            .hasStatus(expected.status)
            .hasGssCode(expected.gssCode)
            .hasPrintRequests(expected.printRequests)
            .hasDateCreated()
            .hasCreatedBy()
            .hasVersion()
    }
}
