package uk.gov.dluhc.printapi.rest

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import uk.gov.dluhc.printapi.config.IntegrationTest
import uk.gov.dluhc.printapi.testsupport.bearerToken
import uk.gov.dluhc.printapi.testsupport.testdata.anotherValidEroId
import uk.gov.dluhc.printapi.testsupport.testdata.getVCAnonymousAdminBearerToken

internal class UpdateAnonymousElectorDocumentStatusIntegrationTest : IntegrationTest() {

    companion object {
        private const val URI_TEMPLATE = "/eros/{ERO_ID}/anonymous-elector-documents/{DOCUMENT_NUMBER}/status"
        private const val DOCUMENT_NUMBER = "A123456789"
    }

    @Test
    fun `should return forbidden given user with valid bearer token belonging to a different ero`() {
        wireMockService.stubCognitoJwtIssuerResponse()
        val userGroupEroId = anotherValidEroId(ERO_ID)

        webTestClient.patch()
            .uri(URI_TEMPLATE, ERO_ID, DOCUMENT_NUMBER)
            .bearerToken(getVCAnonymousAdminBearerToken(eroId = userGroupEroId))
            .contentType(APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isForbidden
    }
}
