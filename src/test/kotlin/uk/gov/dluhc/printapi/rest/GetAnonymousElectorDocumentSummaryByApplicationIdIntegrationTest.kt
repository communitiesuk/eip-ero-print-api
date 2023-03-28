package uk.gov.dluhc.printapi.rest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType.ERO_COLLECTION
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType.REGISTERED
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus.PRINTED
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentSummariesResponse
import uk.gov.dluhc.printapi.models.CertificateLanguage.EN
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.models.SupportingInformationFormat.STANDARD
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElector
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildValidAddress
import java.time.ZoneOffset

internal class GetAnonymousElectorDocumentSummaryByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents?applicationId={APPLICATION_ID}"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4bbc"
        private const val GSS_CODE = "W06000099"
    }

    @Test
    fun `should return bad request given request without applicationId query string parameter`() {
        wireMockService.stubCognitoJwtIssuerResponse()

        webTestClient.get()
            .uri("/eros/{ERO_ID}/anonymous-elector-documents", ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isBadRequest
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return anonymous elector document summary`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val aedMatchingDocument1 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE, sourceType = ANONYMOUS_ELECTOR_DOCUMENT, sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(firstName = "John", middleNames = null, surname = "Jacob"),
            delivery = buildDelivery(deliveryAddressType = REGISTERED)
        )
        anonymousElectorDocumentRepository.save(aedMatchingDocument1)

        // We want to return the records in descending order of [dateCreated].
        Thread.sleep(1000)

        val aedMatchingDocument2 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE, sourceType = ANONYMOUS_ELECTOR_DOCUMENT, sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(firstName = "Mike", middleNames = "William Brown", surname = "Johnson"),
            delivery = buildDelivery(deliveryAddressType = ERO_COLLECTION)
        )
        val aedDocumentWithDifferentApplicationId = buildAnonymousElectorDocument(
            gssCode = GSS_CODE, sourceType = ANONYMOUS_ELECTOR_DOCUMENT, sourceReference = aValidSourceReference()
        )
        val aedDocumentWithDifferentGssCode = buildAnonymousElectorDocument(
            gssCode = getRandomGssCode(), sourceType = ANONYMOUS_ELECTOR_DOCUMENT, sourceReference = APPLICATION_ID
        )
        val aedDocumentWithDifferentSourceType = buildAnonymousElectorDocument(
            gssCode = GSS_CODE, sourceType = SourceType.VOTER_CARD, sourceReference = APPLICATION_ID
        )
        anonymousElectorDocumentRepository.saveAll(
            listOf(
                aedMatchingDocument2,
                aedDocumentWithDifferentApplicationId,
                aedDocumentWithDifferentGssCode,
                aedDocumentWithDifferentSourceType
            )
        )

        val expectedRecord1 = with(aedMatchingDocument1) {
            buildAnonymousElectorDocumentSummary(
                certificateNumber = certificateNumber,
                electoralRollNumber = electoralRollNumber,
                gssCode = gssCode,
                certificateLanguage = EN,
                supportingInformationFormat = STANDARD,
                deliveryAddressType = DeliveryAddressType.REGISTERED,
                elector = with(contactDetails!!) {
                    buildAnonymousElector(
                        addressee = "John Jacob",
                        registeredAddress = with(address!!) {
                            buildValidAddress(
                                property = property,
                                street = street!!,
                                town = town,
                                area = area,
                                locality = locality,
                                uprn = uprn,
                                postcode = postcode!!
                            )
                        }
                    )
                },
                status = PRINTED,
                photoLocation = photoLocationArn,
                issueDate = issueDate,
                userId = userId,
                dateTime = requestDateTime.atOffset(ZoneOffset.UTC),
            )
        }
        val expectedRecord2 = with(aedMatchingDocument2) {
            buildAnonymousElectorDocumentSummary(
                certificateNumber = certificateNumber,
                electoralRollNumber = electoralRollNumber,
                gssCode = gssCode,
                certificateLanguage = EN,
                supportingInformationFormat = STANDARD,
                deliveryAddressType = DeliveryAddressType.ERO_MINUS_COLLECTION,
                elector = with(contactDetails!!) {
                    buildAnonymousElector(
                        addressee = "Mike William Brown Johnson",
                        registeredAddress = with(address!!) {
                            buildValidAddress(
                                property = property,
                                street = street!!,
                                town = town,
                                area = area,
                                locality = locality,
                                uprn = uprn,
                                postcode = postcode!!
                            )
                        }
                    )
                },
                status = PRINTED,
                photoLocation = photoLocationArn,
                issueDate = issueDate,
                userId = userId,
                dateTime = requestDateTime.atOffset(ZoneOffset.UTC),
            )
        }

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AnonymousElectorDocumentSummariesResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.anonymousElectorDocuments).isNotNull.isNotEmpty
            .usingRecursiveComparison()
            .isEqualTo(listOf(expectedRecord2, expectedRecord1))
    }
}
