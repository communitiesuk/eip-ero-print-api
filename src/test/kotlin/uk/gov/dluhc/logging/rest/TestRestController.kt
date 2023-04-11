package uk.gov.dluhc.logging.rest

import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

private val logger = KotlinLogging.logger {}

/**
 * A simple REST Controller exposing a single endpoint to be used by [CorrelationIdMdcInterceptorTest]
 */
@RestController
class TestRestController {

    @GetMapping("/correlation-id-test-api")
    fun correlationIdTestEndpoint() {
        logger.info { "Test API successfully called" }
    }
}
