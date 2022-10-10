package uk.gov.dluhc.printapi.testsupport

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.springframework.stereotype.Service

/**
 * Service class to provide support to tests with setting up and managing wiremock stubs
 */
@Service
class WiremockService(private val wireMockServer: WireMockServer) {
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
}
