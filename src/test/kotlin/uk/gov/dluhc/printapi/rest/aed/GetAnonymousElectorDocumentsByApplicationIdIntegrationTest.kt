package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentDelivery
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType.ERO_COLLECTION
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType.REGISTERED
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentsResponse
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat.LARGE_MINUS_PRINT
import uk.gov.dluhc.printapi.models.DeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedDelivery
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElectorApi
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildAnonymousElectorDocumentApi
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildValidAddress
import java.time.ZoneOffset

internal class GetAnonymousElectorDocumentsByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents?applicationId={APPLICATION_ID}"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4bbc"
        private const val APPLICATION_REFERENCE = "V3JSZC4CRH"
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
    fun `should return both fully retained and initial data removed anonymous elector documents by application ID, with newest documents first`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val aedMatchingDocument1 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            applicationReference = APPLICATION_REFERENCE,
            supportingInformationFormat = SupportingInformationFormat.LARGE_PRINT,
            contactDetails = buildAedContactDetails(firstName = "John", middleNames = null, surname = "Jacob"),
            delivery = buildAedDelivery(deliveryAddressType = REGISTERED)
        )
        anonymousElectorDocumentRepository.save(aedMatchingDocument1)

        // We want to return the records in descending order of [dateCreated].
        Thread.sleep(1000)

        val aedMatchingDocument2WithInitialDataRemoved = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            applicationReference = APPLICATION_REFERENCE,
            supportingInformationFormat = SupportingInformationFormat.STANDARD,
            contactDetails = buildAedContactDetails(
                firstName = "Mike",
                middleNames = "William Brown",
                surname = "Johnson"
            ),
            delivery = buildAedDelivery(deliveryAddressType = ERO_COLLECTION, collectionReason = "Away from home")
        ).also { it.removeInitialRetentionPeriodData() }
        val aedDocumentWithDifferentApplicationId = buildAnonymousElectorDocument(
            gssCode = GSS_CODE, sourceReference = aValidSourceReference()
        )
        val aedDocumentWithDifferentGssCode = buildAnonymousElectorDocument(
            gssCode = getRandomGssCode(), sourceReference = APPLICATION_ID
        )
        val aedDocumentWithDifferentSourceType = buildAnonymousElectorDocument(
            gssCode = GSS_CODE, sourceType = SourceType.VOTER_CARD, sourceReference = APPLICATION_ID
        )
        anonymousElectorDocumentRepository.saveAll(
            listOf(
                aedMatchingDocument2WithInitialDataRemoved, aedDocumentWithDifferentApplicationId,
                aedDocumentWithDifferentGssCode, aedDocumentWithDifferentSourceType,
            )
        )

        val expectedPhotoUrl = "http://localhost:8080/eros/$ERO_ID/anonymous-elector-documents/photo?applicationId=$APPLICATION_ID"

        val expectedFirstRecord = with(aedMatchingDocument2WithInitialDataRemoved) {
            buildAnonymousElectorDocumentApi(
                certificateNumber = certificateNumber, electoralRollNumber = electoralRollNumber,
                gssCode = gssCode, deliveryAddressType = null,
                collectionReason = null,
                sourceReference = sourceReference, applicationReference = applicationReference,
                elector = with(contactDetails!!) {
                    buildAnonymousElectorApi(
                        firstName = firstName, middleNames = middleNames, surname = surname,
                        addressee = "Mike William Brown Johnson",
                        registeredAddress = null,
                        email = null,
                        phoneNumber = null
                    )
                },
                photoUrl = expectedPhotoUrl,
                issueDate = issueDate,
                userId = userId,
                dateTime = requestDateTime.atOffset(ZoneOffset.UTC),
                supportingInformationFormat = null,
                status = AnonymousElectorDocumentStatus.EXPIRED,
            )
        }
        val expectedSecondRecord = with(aedMatchingDocument1) {
            buildAnonymousElectorDocumentApi(
                certificateNumber = certificateNumber, electoralRollNumber = electoralRollNumber,
                gssCode = gssCode, deliveryAddressType = DeliveryAddressType.REGISTERED,
                collectionReason = null,
                sourceReference = sourceReference, applicationReference = applicationReference,
                elector = with(contactDetails!!) {
                    buildAnonymousElectorApi(
                        firstName = firstName, middleNames = middleNames, surname = surname,
                        addressee = "John Jacob",
                        registeredAddress = with(address!!) {
                            buildValidAddress(
                                property = property, street = street!!,
                                town = town, area = area, locality = locality,
                                uprn = uprn, postcode = postcode!!
                            )
                        },
                        email = email,
                        phoneNumber = phoneNumber
                    )
                },
                photoUrl = expectedPhotoUrl,
                issueDate = issueDate,
                userId = userId,
                dateTime = requestDateTime.atOffset(ZoneOffset.UTC),
                supportingInformationFormat = LARGE_MINUS_PRINT
            )
        }

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AnonymousElectorDocumentsResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.anonymousElectorDocuments).isNotNull.isNotEmpty
            .usingRecursiveComparison()
            .isEqualTo(listOf(expectedFirstRecord, expectedSecondRecord))
    }

    @Test
    fun `should return anonymous elector documents that only have deliveryAddressType by application ID`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val aed1 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            applicationReference = APPLICATION_REFERENCE,
            contactDetails = buildAedContactDetails(firstName = "John", middleNames = null, surname = "Jacob"),
        ).also { it.removeInitialRetentionPeriodData() }
        anonymousElectorDocumentRepository.save(aed1)

        // We want to return the records in descending order of [dateCreated].
        Thread.sleep(1000)

        val aed2 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            applicationReference = APPLICATION_REFERENCE,
            contactDetails = buildAedContactDetails(firstName = "John", middleNames = null, surname = "Jacob"),
        )
            .also { it.removeInitialRetentionPeriodData() }
            .apply { delivery = AnonymousElectorDocumentDelivery(deliveryAddressType = ERO_COLLECTION) }
        anonymousElectorDocumentRepository.save(aed2)

        val expectedPhotoUrl =
            "http://localhost:8080/eros/$ERO_ID/anonymous-elector-documents/photo?applicationId=$APPLICATION_ID"

        val expectedFirstRecord = with(aed2) {
            buildAnonymousElectorDocumentApi(
                certificateNumber = certificateNumber, electoralRollNumber = electoralRollNumber,
                gssCode = gssCode, deliveryAddressType = DeliveryAddressType.ERO_MINUS_COLLECTION,
                collectionReason = null,
                sourceReference = sourceReference, applicationReference = applicationReference,
                elector = with(contactDetails!!) {
                    buildAnonymousElectorApi(
                        firstName = firstName, middleNames = middleNames, surname = surname,
                        addressee = "John Jacob",
                        registeredAddress = null,
                        email = null,
                        phoneNumber = null
                    )
                },
                photoUrl = expectedPhotoUrl,
                issueDate = issueDate,
                userId = userId,
                dateTime = requestDateTime.atOffset(ZoneOffset.UTC),
                supportingInformationFormat = null,
                status = AnonymousElectorDocumentStatus.EXPIRED,
            )
        }
        val expectedSecondRecord = with(aed1) {
            buildAnonymousElectorDocumentApi(
                certificateNumber = certificateNumber, electoralRollNumber = electoralRollNumber,
                gssCode = gssCode, deliveryAddressType = null,
                collectionReason = null,
                sourceReference = sourceReference, applicationReference = applicationReference,
                elector = with(contactDetails!!) {
                    buildAnonymousElectorApi(
                        firstName = firstName, middleNames = middleNames, surname = surname,
                        addressee = "John Jacob",
                        registeredAddress = null,
                        email = null,
                        phoneNumber = null,
                    )
                },
                photoUrl = expectedPhotoUrl,
                issueDate = issueDate,
                userId = userId,
                dateTime = requestDateTime.atOffset(ZoneOffset.UTC),
                supportingInformationFormat = null,
                status = AnonymousElectorDocumentStatus.EXPIRED,
            )
        }

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AnonymousElectorDocumentsResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual).isNotNull
        assertThat(actual!!.anonymousElectorDocuments).isNotNull.isNotEmpty
            .usingRecursiveComparison()
            .isEqualTo(listOf(expectedFirstRecord, expectedSecondRecord))
    }

    @Test
    fun `should return empty list given no AEDs exist for application ID`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        // When
        val response = webTestClient.get()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk
            .returnResult(AnonymousElectorDocumentsResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        assertThat(actual!!.anonymousElectorDocuments).isEmpty()
    }
}
