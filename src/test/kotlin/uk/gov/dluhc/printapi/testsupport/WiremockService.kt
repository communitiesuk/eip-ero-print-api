package uk.gov.dluhc.printapi.testsupport

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficesResponse

/**
 * Service class to provide support to tests with setting up and managing wiremock stubs
 */
@Service
class WiremockService(private val wireMockServer: WireMockServer) {
    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private var baseUrl: String? = null

    fun resetAllStubsAndMappings() {
        wireMockServer.resetAll()
    }

    fun wiremockBaseUrl(): String {
        if (baseUrl == null) {
            baseUrl = "http://localhost:${wireMockServer.port()}"
        }
        return baseUrl!!
    }

    fun stubCognitoJwtIssuerResponse() {
        wireMockServer.stubFor(
            get(urlPathMatching("/cognito/.well-known/jwks.json")).willReturn(
                ok().withBody(
                    """
                            {
                               "keys":[
                                    ${RsaKeyPair.jwk.toJSONString()}
                               ]
                            }
                    """.trimIndent()
                )
            )
        )
    }

    fun stubEroManagementGetEroByGssCode(ero: ElectoralRegistrationOfficeResponse, gssCode: String) {
        val responseBody = objectMapper.writeValueAsString(ElectoralRegistrationOfficesResponse(listOf(ero)))
        wireMockServer.stubFor(
            get(urlEqualTo("/ero-management-api/eros?gssCode=$gssCode"))
                .willReturn(
                    ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }

    fun stubEroManagementGetEroByEroId(ero: ElectoralRegistrationOfficeResponse, eroId: String) {
        val responseBody = objectMapper.writeValueAsString(ero)
        wireMockServer.stubFor(
            get(urlEqualTo("/ero-management-api/eros/$eroId"))
                .willReturn(
                    ResponseDefinitionBuilder.responseDefinition()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)
                )
        )
    }

    fun verifyEroManagementGetEro(gssCode: String) {
        wireMockServer.verify(1, getRequestedFor(urlEqualTo("/ero-management-api/eros?gssCode=$gssCode")))
    }
}
