package uk.gov.dluhc.printapi.config

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WiremockConfiguration {

    var baseUrl: String? = null

    @Bean
    fun wireMockServer(applicationContext: ConfigurableApplicationContext): WireMockServer =
        WireMockServer(
            WireMockConfiguration.options()
                .dynamicPort()
                .dynamicHttpsPort()
        ).apply {
            start()
            baseUrl = "http://localhost:${this.port()}"
            TestPropertyValues.of(
                "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:${this.port()}/cognito/.well-known/jwks.json",
            ).applyTo(applicationContext)
        }
}
