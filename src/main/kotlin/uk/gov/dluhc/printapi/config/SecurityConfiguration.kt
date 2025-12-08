package uk.gov.dluhc.printapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import uk.gov.dluhc.internalauth.presignedStsQueryParametersAuthenticationFilter

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
    private val environment: Environment,
    private val cognitoJwtAuthenticationConverter: CognitoJwtAuthenticationConverter
) {

    @Bean
    fun filterChain(
        http: HttpSecurity,
        objectMapper: ObjectMapper,
        @Value("\${sts.base-url}") stsBaseUrl: String,
    ): SecurityFilterChain =
        http.also { httpSecurity ->
            httpSecurity
                .sessionManagement {
                    it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                }
                .exceptionHandling {
                    it.authenticationEntryPoint { _, response, _ ->
                        response.status = UNAUTHORIZED.value()
                    }
                    it.accessDeniedHandler { _, response, _ ->
                        response.status = FORBIDDEN.value()
                    }
                }
                .cors { }
                .formLogin { it.disable() }
                .httpBasic { it.disable() }
                .authorizeHttpRequests {
                    it.requestMatchers(OPTIONS).permitAll()
                    it.requestMatchers("/actuator/**").permitAll()

                    // These requests are authenticated through the API gateway using IAM
                    it.requestMatchers("/certificates/statistics", "/anonymous-elector-documents/statistics")
                        .permitAll()

                    it.anyRequest().authenticated()
                }
                .oauth2ResourceServer { oAuth2ResourceServerConfigurer ->
                    oAuth2ResourceServerConfigurer.jwt {
                        it.jwtAuthenticationConverter(cognitoJwtAuthenticationConverter)
                        it.jwkSetUri(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                    }
                }
                .addFilter(presignedStsQueryParametersAuthenticationFilter(stsBaseUrl, objectMapper))
        }.build()
}
