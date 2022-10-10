package uk.gov.dluhc.printapi.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component

/**
 * Custom JWT Converter implementation that returns a Spring Security Authentication Token based on the claims within
 * the Cognito JWT.
 *
 * Specifically this pulls the group names from the claim `cognito:groups` and uses them as the Authentication authorities
 * so that Spring Security can recognise the group/role membership via `@PreAuthorise('hasRole=....')`
 *
 * It also sets the principal name from the JWT email (the user's cognito username, which is their email address)
 * rather than the JWT subject (which is a cognito internal ID)
 */
@Component
class CognitoJwtAuthenticationConverter : Converter<Jwt, AbstractAuthenticationToken> {

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities: List<GrantedAuthority> = jwt.claims["cognito:groups"]?.let {
            it as Collection<*>
        }?.map {
            SimpleGrantedAuthority(it.toString())
        } ?: emptyList()
        return JwtAuthenticationToken(jwt, authorities, jwt.email)
    }

    /**
     * Return the user's email address from the JWT.
     * JWTs presented by AWS Cognito have a claim called `email`
     */
    private val Jwt.email: String
        get() = claims["email"]?.toString()
            ?: throw IllegalStateException("Email claim not found in JWT")
}
