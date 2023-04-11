package uk.gov.dluhc.logging.config

import org.slf4j.MDC
import java.util.UUID

/**
 * MVC Interceptor and AOP beans that set the correlation ID MDC variable for inclusion in all log statements.
 */

const val CORRELATION_ID = "correlationId"
const val CORRELATION_ID_HEADER = "x-correlation-id"

fun generateCorrelationId(): String =
    UUID.randomUUID().toString().replace("-", "")

fun getCurrentCorrelationId(): String =
    MDC.get(CORRELATION_ID) ?: generateCorrelationId()
