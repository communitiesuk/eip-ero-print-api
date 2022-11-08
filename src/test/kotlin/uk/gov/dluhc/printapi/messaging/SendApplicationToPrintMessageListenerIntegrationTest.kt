package uk.gov.dluhc.printapi.messaging

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.CertificateDelivery
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildSendApplicationToPrintMessage
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
        val expected = with(payload) {
            PrintDetails(
                id = UUID.randomUUID(),
                requestId = aValidRequestId(),
                sourceReference = sourceReference,
                applicationReference = applicationReference,
                sourceType = SourceType.VOTER_CARD,
                vacNumber = aValidVacNumber(),
                requestDateTime = requestDateTime,
                applicationReceivedDateTime = applicationReceivedDateTime,
                firstName = firstName,
                middleNames = middleNames,
                surname = surname,
                certificateLanguage = CertificateLanguage.EN,
                photoLocation = photoLocation,
                delivery = with(delivery) {
                    CertificateDelivery(
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
                        deliveryClass = uk.gov.dluhc.printapi.database.entity.DeliveryClass.STANDARD,
                        deliveryMethod = uk.gov.dluhc.printapi.database.entity.DeliveryMethod.DELIVERY,
                    )
                },
                gssCode = gssCode,
                issuingAuthority = localAuthority.name,
                issueDate = LocalDate.now(),
                eroEnglish = with(ero) {
                    ElectoralRegistrationOffice(
                        name = name,
                        phoneNumber = "",
                        emailAddress = "",
                        website = "",
                        address = Address(
                            street = "",
                            postcode = ""
                        )
                    )
                },
                eroWelsh = null
            )
        }

        // When
        sqsMessagingTemplate.convertAndSend(sendApplicationToPrintQueueName, payload)

        // Then
        await.atMost(5, SECONDS).untilAsserted {
            wireMockService.verifyEroManagementGetEro(gssCode)
            val response = dynamoDbClient.scan(
                ScanRequest.builder().tableName(dynamoDbConfiguration.printDetailsTableName).build()
            )
            assertThat(response.items().count()).isEqualTo(1)
            val id = UUID.fromString(response.items()[0]["id"]!!.s())
            val saved = printDetailsRepository.get(id)
            assertThat(saved).usingRecursiveComparison().ignoringFields("id", "requestId", "vacNumber")
                .isEqualTo(expected)
            assertThat(saved.requestId).containsPattern(Regex("^[a-f\\d]{24}$").pattern)
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
