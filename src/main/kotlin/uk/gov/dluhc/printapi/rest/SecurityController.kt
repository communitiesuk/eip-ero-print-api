package uk.gov.dluhc.printapi.rest

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Simple controller to allow tests of spring security whilst we don't have other controllers
 * TODO - delete this controller and associated tests when we implement controllers to support stories
 */
@RestController
class SecurityController {

    @GetMapping("/secured-endpoint")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("isAuthenticated()")
    fun securedEndpoint(authentication: Authentication): String {
        return "Hello ${authentication.name}"
    }
}
