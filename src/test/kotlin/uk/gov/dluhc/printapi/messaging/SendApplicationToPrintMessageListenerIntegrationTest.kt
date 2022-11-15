package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.CertificateFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.Delivery
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.rds.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

internal class SendApplicationToPrintMessageListenerIntegrationTest : IntegrationTest() {

    @Test
    fun `should process message received on queue`() {
        // Given
        val ero = buildElectoralRegistrationOfficeResponse()
        val localAuthority = ero.localAuthorities[1]
        val gssCode = localAuthority.gssCode!!
        val payload = buildSendApplicationToPrintMessage(gssCode = gssCode)

        wireMockService.stubEroManagementGetEro(ero, gssCode)
        // TODO - when ERO Management returns contact data wiremock will return relevant contact details and the expected ERO can reflect this
        val expectedEnglishEro = ElectoralRegistrationOffice(
            name = "Gwynedd Council Elections",
            phoneNumber = "01766 771000",
            website = "https://www.gwynedd.llyw.cymru/en/Council/Contact-us/Contact-us.aspx",
            emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
            address = Address(
                property = "Gwynedd Council Headquarters",
                street = "Shirehall Street",
                town = "Caernarfon",
                area = "Gwynedd",
                postcode = "LL55 1SH",
            )
        )

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
            val printRequest = PrintRequest(
                requestId = aValidRequestId(),
                vacVersion = "1",
                requestDateTime = requestDateTime.toInstant(),
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = CertificateLanguage.EN,
                certificateFormat = CertificateFormat.STANDARD,
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
                        deliveryMethod = DeliveryMethod.DELIVERY,
                    )
                },
                eroEnglish = expectedEnglishEro,
                eroWelsh = null,
                statusHistory = mutableListOf(
                    PrintRequestStatus(
                        status = Status.PENDING_ASSIGNMENT_TO_BATCH,
                        eventDateTime = Instant.now().minusSeconds(10)
                    )
                ),
                userId = userId
            )
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
            assertThat(saved).usingRecursiveComparison()
                .ignoringFields(
                    "id",
                    "version",
                    "createdBy",
                    "dateCreated",
                    "vacNumber",
                    "applicationReceivedDateTime",
                    "printRequests.id",
                    "printRequests.version",
                    "printRequests.dateCreated",
                    "printRequests.createdBy",
                    "printRequests.requestId",
                    "printRequests.requestDateTime",
                    "printRequests.eroEnglish.id",
                    "printRequests.eroEnglish.version",
                    "printRequests.eroEnglish.dateCreated",
                    "printRequests.eroEnglish.createdBy",
                    "printRequests.eroEnglish.address.id",
                    "printRequests.eroEnglish.address.version",
                    "printRequests.eroEnglish.address.dateCreated",
                    "printRequests.eroEnglish.address.createdBy",
                    "printRequests.delivery.id",
                    "printRequests.delivery.version",
                    "printRequests.delivery.dateCreated",
                    "printRequests.delivery.createdBy",
                    "printRequests.delivery.address.id",
                    "printRequests.delivery.address.version",
                    "printRequests.delivery.address.dateCreated",
                    "printRequests.delivery.address.createdBy",
                    "printRequests.statusHistory.id",
                    "printRequests.statusHistory.version",
                    "printRequests.statusHistory.dateCreated",
                    "printRequests.statusHistory.createdBy",
                    "printRequests.statusHistory.eventDateTime"
                )
                .isEqualTo(expected)
            assertThat(saved.status).isEqualTo(Status.PENDING_ASSIGNMENT_TO_BATCH)
            assertThat(saved.getCurrentPrintRequest().requestId).containsPattern(Regex("^[a-f\\d]{24}$").pattern)
            assertThat(saved.vacNumber).containsPattern(Regex("^[A-Za-z\\d]{20}$").pattern)
        }
    }

    @Test
    fun `should not process message that does conform to validation constraints`() {
        // Given
        val payload = buildSendApplicationToPrintMessage(gssCode = "ABC") // gssCode must be 9 characters

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.during(5, SECONDS).until {
            val response = dynamoDbClient.scan(
                ScanRequest.builder().tableName(dynamoDbConfiguration.printDetailsTableName).build()
            )
            assertThat(response.items()).isEmpty()
            true
        }
    }
}
