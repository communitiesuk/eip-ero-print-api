package uk.gov.dluhc.printapi.testsupport

import org.springframework.test.web.reactive.server.WebTestClient

fun WebTestClient.RequestHeadersSpec<*>.bearerToken(bearerToken: String): WebTestClient.RequestBodySpec =
    header("authorization", bearerToken) as WebTestClient.RequestBodySpec
