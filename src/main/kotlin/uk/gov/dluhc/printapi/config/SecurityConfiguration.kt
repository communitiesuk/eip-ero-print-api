package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod.OPTIONS
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
    private val environment: Environment,
    private val cognitoJwtAuthenticationConverter: CognitoJwtAuthenticationConverter
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain =
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
                .authorizeRequests {
                    it.antMatchers(OPTIONS).permitAll()
                    it.antMatchers("/actuator/**").permitAll()
                    it.anyRequest().authenticated()
                }
                .oauth2ResourceServer { oAuth2ResourceServerConfigurer ->
                    oAuth2ResourceServerConfigurer.jwt {
                        it.jwtAuthenticationConverter(cognitoJwtAuthenticationConverter)
                        it.jwkSetUri(environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                    }
                }
        }.build()
}
