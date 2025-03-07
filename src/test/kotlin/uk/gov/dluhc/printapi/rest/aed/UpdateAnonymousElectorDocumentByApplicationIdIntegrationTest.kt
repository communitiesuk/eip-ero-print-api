package uk.gov.dluhc.printapi.rest.aed

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentDelivery
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.models.ErrorResponse
import uk.gov.dluhc.printapi.testsupport.assertj.assertions.models.ErrorResponseAssert
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAedContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.entity.buildAnonymousElectorDocument
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildUpdateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.testsupport.withBody
import java.time.temporal.ChronoUnit

internal class UpdateAnonymousElectorDocumentByApplicationIdIntegrationTest : IntegrationTest() {
    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents"
        private const val GSS_CODE = "W06000099"
        private const val APPLICATION_ID = "7762ccac7c056046b75d4bbc"
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .withBody(buildUpdateAnonymousElectorDocumentRequest())
            .exchange()
            .expectStatus()
            .isForbidden
    }

    @Test
    fun `should return bad request given invalid request body`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val invalidRequestBody = buildUpdateAnonymousElectorDocumentRequest(
            email = null,
            phoneNumber = null
        )

        // When
        val response = webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(invalidRequestBody)
            .exchange()
            .expectStatus()
            .isBadRequest
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessageContaining("Either email or phoneNumber must be provided")
    }

    @Test
    fun `should return not found given no AEDs exist for application`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val unknownSourceReference = aValidSourceReference()

        // When
        val response = webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(buildUpdateAnonymousElectorDocumentRequest(sourceReference = unknownSourceReference))
            .exchange()
            .expectStatus()
            .isNotFound
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(404)
            .hasError("Not Found")
            .hasMessage("Certificate for eroId = $ERO_ID with sourceType = ANONYMOUS_ELECTOR_DOCUMENT and sourceReference = $unknownSourceReference not found")
    }

    @Test
    fun `should not update elector's email and phone number if all certificates of an application has expired`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val aed1 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails()
        ).also { it.removeInitialRetentionPeriodData() }
        anonymousElectorDocumentRepository.save(aed1)
        Thread.sleep(1000)

        val aed2 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails()
        )
            .also { it.removeInitialRetentionPeriodData() }
            .apply { delivery = AnonymousElectorDocumentDelivery(deliveryAddressType = DeliveryAddressType.ERO_COLLECTION) }
        anonymousElectorDocumentRepository.save(aed2)

        val newEmailAddress = anotherValidEmailAddress()
        val newPhoneNumber = anotherValidPhoneNumber()
        val updateAedRequest = buildUpdateAnonymousElectorDocumentRequest(
            sourceReference = APPLICATION_ID,
            email = newEmailAddress,
            phoneNumber = newPhoneNumber
        )

        // When
        val response = webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(updateAedRequest)
            .exchange()
            .expectStatus()
            .is4xxClientError
            .returnResult(ErrorResponse::class.java)

        // Then
        val actual = response.responseBody.blockFirst()
        ErrorResponseAssert.assertThat(actual)
            .hasStatus(400)
            .hasError("Bad Request")
            .hasMessage("All certificate for eroId = $ERO_ID with sourceReference = $APPLICATION_ID have pass the initial retention period and cannot be updated.")
    }

    @Test
    fun `should not update elector's email and phone number for AEDs with initial retention period data deleted`() {
        // Given
        wireMockService.stubCognitoJwtIssuerResponse()
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE))
        )
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)

        val aed1WithInitialDataRemoved = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails()
        ).also { it.removeInitialRetentionPeriodData() }
        anonymousElectorDocumentRepository.save(aed1WithInitialDataRemoved)
        Thread.sleep(1500)

        val dateUpdated1 = aed1WithInitialDataRemoved.contactDetails!!.dateUpdated!!
        val updatedBy1 = aed1WithInitialDataRemoved.contactDetails!!.updatedBy!!

        val originalEmailAddress = aValidEmailAddress()
        val originalPhoneNumber = aValidPhoneNumber()
        val aed2 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        anonymousElectorDocumentRepository.save(aed2)

        val dateCreated2 = aed2.contactDetails!!.dateCreated!!
        val createdBy2 = aed2.contactDetails!!.createdBy!!
        val dateUpdated2 = aed2.contactDetails!!.dateUpdated!!
        val expectedUpdatedBy2 = "an-ero-user1@$ERO_ID.gov.uk"
        val newEmailAddress = anotherValidEmailAddress()
        val newPhoneNumber = anotherValidPhoneNumber()
        val updateAedRequest = buildUpdateAnonymousElectorDocumentRequest(
            sourceReference = APPLICATION_ID,
            email = newEmailAddress,
            phoneNumber = newPhoneNumber
        )

        // When
        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(updateAedRequest)
            .exchange()
            .expectStatus()
            .isNoContent

        // Then
        val updated1 = anonymousElectorDocumentRepository.findById(aed1WithInitialDataRemoved.id!!).get()
        assertThat(updated1.contactDetails!!.email).isNull()
        assertThat(updated1.contactDetails!!.phoneNumber).isNull()
        assertThat(updated1.contactDetails!!.dateUpdated).isCloseTo(dateUpdated1, within(1, ChronoUnit.SECONDS))
        assertThat(updated1.contactDetails!!.updatedBy).isEqualTo(updatedBy1)

        val updated2 = anonymousElectorDocumentRepository.findById(aed2.id!!).get()
        assertThat(updated2.contactDetails!!.email).isEqualTo(newEmailAddress)
        assertThat(updated2.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
        assertThat(updated2.contactDetails!!.dateCreated).isCloseTo(dateCreated2, within(1, ChronoUnit.SECONDS))
        assertThat(updated2.contactDetails!!.createdBy).isEqualTo(createdBy2)
        assertThat(updated2.contactDetails!!.dateUpdated).isAfter(dateUpdated2)
        assertThat(updated2.contactDetails!!.updatedBy).isEqualTo(expectedUpdatedBy2)
    }

    @Test
    fun `should update elector's email and phone number`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val originalEmailAddress = aValidEmailAddress()
        val originalPhoneNumber = aValidPhoneNumber()
        val aed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        anonymousElectorDocumentRepository.save(aed)
        Thread.sleep(1500)

        val dateCreated = aed.contactDetails!!.dateCreated!!
        val createdBy = aed.contactDetails!!.createdBy!!
        val dateUpdated = aed.contactDetails!!.dateUpdated!!
        val expectedUpdatedBy = "an-ero-user1@$ERO_ID.gov.uk"
        val newEmailAddress = anotherValidEmailAddress()
        val newPhoneNumber = anotherValidPhoneNumber()
        val updateAedRequest = buildUpdateAnonymousElectorDocumentRequest(
            sourceReference = APPLICATION_ID,
            email = newEmailAddress,
            phoneNumber = newPhoneNumber
        )

        // When
        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(updateAedRequest)
            .exchange()
            .expectStatus().isNoContent

        // Then
        val updated = anonymousElectorDocumentRepository.findById(aed.id!!).get()
        assertThat(updated.contactDetails!!.email).isEqualTo(newEmailAddress)
        assertThat(updated.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
        assertThat(updated.contactDetails!!.dateCreated).isCloseTo(dateCreated, within(1, ChronoUnit.SECONDS))
        assertThat(updated.contactDetails!!.createdBy).isEqualTo(createdBy)
        assertThat(updated.contactDetails!!.dateUpdated).isAfter(dateUpdated)
        assertThat(updated.contactDetails!!.updatedBy).isEqualTo(expectedUpdatedBy)
    }

    @Test
    fun `should not update elector's email and phone number given same existing values`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val originalEmailAddress = aValidEmailAddress()
        val originalPhoneNumber = aValidPhoneNumber()
        val aed = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        anonymousElectorDocumentRepository.save(aed)
        Thread.sleep(1000)

        val dateCreated = aed.contactDetails!!.dateCreated!!
        val createdBy = aed.contactDetails!!.createdBy!!
        val dateUpdated = aed.contactDetails!!.dateUpdated!!
        val updatedBy = aed.contactDetails!!.updatedBy!!
        val updateAedRequest = buildUpdateAnonymousElectorDocumentRequest(
            sourceReference = APPLICATION_ID,
            email = originalEmailAddress,
            phoneNumber = originalPhoneNumber
        )

        // When
        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(updateAedRequest)
            .exchange()
            .expectStatus().isNoContent

        // Then
        val updated = anonymousElectorDocumentRepository.findById(aed.id!!).get()
        assertThat(updated.contactDetails!!.email).isEqualTo(originalEmailAddress)
        assertThat(updated.contactDetails!!.phoneNumber).isEqualTo(originalPhoneNumber)
        assertThat(updated.contactDetails!!.dateCreated).isCloseTo(dateCreated, within(1, ChronoUnit.SECONDS))
        assertThat(updated.contactDetails!!.createdBy).isEqualTo(createdBy)
        assertThat(updated.contactDetails!!.dateUpdated).isCloseTo(dateUpdated, within(1, ChronoUnit.SECONDS))
        assertThat(updated.contactDetails!!.updatedBy).isEqualTo(updatedBy)
    }

    @Test
    fun `should update elector's email and phone number on multiple AEDs`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val originalEmailAddress = aValidEmailAddress()
        val originalPhoneNumber = aValidPhoneNumber()
        val aed1 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        val aed2 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2))

        val newEmailAddress = anotherValidEmailAddress()
        val newPhoneNumber = anotherValidPhoneNumber()
        val updateAedRequest = buildUpdateAnonymousElectorDocumentRequest(
            sourceReference = APPLICATION_ID,
            email = newEmailAddress,
            phoneNumber = newPhoneNumber
        )

        // When
        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(updateAedRequest)
            .exchange()
            .expectStatus().isNoContent

        // Then
        val updated1 = anonymousElectorDocumentRepository.findById(aed1.id!!).get()
        val updated2 = anonymousElectorDocumentRepository.findById(aed2.id!!).get()
        assertThat(updated1.contactDetails!!.email).isEqualTo(newEmailAddress)
        assertThat(updated1.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
        assertThat(updated2.contactDetails!!.email).isEqualTo(newEmailAddress)
        assertThat(updated2.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
    }

    @Test
    fun `should only update elector's email and phone number on AEDs that have not had initial retention period data deleted `() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            id = ERO_ID,
            localAuthorities = listOf(buildLocalAuthorityResponse(gssCode = GSS_CODE), buildLocalAuthorityResponse())
        )
        wireMockService.stubCognitoJwtIssuerResponse()
        wireMockService.stubEroManagementGetEroByEroId(eroResponse, ERO_ID)
        val originalEmailAddress = aValidEmailAddress()
        val originalPhoneNumber = aValidPhoneNumber()
        val aed1 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        aed1.removeInitialRetentionPeriodData()
        val aed2 = buildAnonymousElectorDocument(
            gssCode = GSS_CODE,
            sourceReference = APPLICATION_ID,
            contactDetails = buildAedContactDetails(email = originalEmailAddress, phoneNumber = originalPhoneNumber)
        )
        anonymousElectorDocumentRepository.saveAll(listOf(aed1, aed2))

        val newEmailAddress = anotherValidEmailAddress()
        val newPhoneNumber = anotherValidPhoneNumber()
        val updateAedRequest = buildUpdateAnonymousElectorDocumentRequest(
            sourceReference = APPLICATION_ID,
            email = newEmailAddress,
            phoneNumber = newPhoneNumber
        )

        // When
        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID, APPLICATION_ID)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = ERO_ID))
            .contentType(APPLICATION_JSON)
            .withBody(updateAedRequest)
            .exchange()
            .expectStatus().isNoContent

        // Then
        val updated1 = anonymousElectorDocumentRepository.findById(aed1.id!!).get()
        val updated2 = anonymousElectorDocumentRepository.findById(aed2.id!!).get()
        assertThat(updated1.contactDetails!!.email).isNull()
        assertThat(updated1.contactDetails!!.phoneNumber).isNull()
        assertThat(updated2.contactDetails!!.email).isEqualTo(newEmailAddress)
        assertThat(updated2.contactDetails!!.phoneNumber).isEqualTo(newPhoneNumber)
    }
}
