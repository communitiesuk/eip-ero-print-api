package uk.gov.dluhc.printapi.testsupport.testdata

import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

fun anAuthenticatedJwtAuthenticationToken(username: String): JwtAuthenticationToken {
    val jwt = Jwt
        .withTokenValue("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQHdpbHRzaGlyZS5nb3YudWsiLCJpYXQiOjE1MTYyMzkwMjIsImF1dGhvcml0aWVzIjpbImVyby1hZG1pbiJdfQ.-pxW8z2xb-AzNLTRP_YRnm9fQDcK6CLt6HimtS8VcDY")
        .header("alg", "HS256").header("typ", "JWT")
        .claim("sub", username)
        .build()
    val authorities = setOf(SimpleGrantedAuthority("ero-admin-1234"))
    return JwtAuthenticationToken(jwt, authorities)
}
