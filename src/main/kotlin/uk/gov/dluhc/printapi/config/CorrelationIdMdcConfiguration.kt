package uk.gov.dluhc.printapi.config

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.lang.Exception
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * MVC Interceptor and AOP beans that set the correlation ID MDC variable for inclusion in all log statements.
 */

const val CORRELATION_ID = "correlationId"
const val CORRELATION_ID_HEADER = "x-correlation-id"

/**
 * MVC Interceptor that sets the correlation ID MDC variable of either a new value, or the value found in the
 * HTTP header `x-correlation-id` if set. This allows for passing and logging a consistent correlation ID between
 * disparate systems or processes.
 */
@Component
class CorrelationIdMdcInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        MDC.put(CORRELATION_ID, request.getHeader(CORRELATION_ID_HEADER) ?: generateCorrelationId())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?
    ) {
        MDC.remove(CORRELATION_ID)
    }
}

/**
 * AOP Aspect to read and set the correlation ID on inbound (received) and outbound SQS [Message]s respectively.
 * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
 */
@Aspect
@Component
class CorrelationIdMdcMessageListenerAspect {

    /**
     * Pointcut for inbound [Message]s (ie. SQS Message's being directed to a listener class) that sets the correlation ID
     * MDC variable to the value found in the Message header `x-correlation-id` if set, or a new value.
     * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
     */
    @Before("execution(* org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler.handleMessage(..))")
    fun beforeHandleMessage(joinPoint: JoinPoint) {
        val message = joinPoint.args[0] as Message<*>?
        MDC.put(CORRELATION_ID, message?.headers?.get(CORRELATION_ID_HEADER)?.toString() ?: generateCorrelationId())
    }

    /**
     * Pointcut for outbound [Message]s (ie. SQS Message's being sent) that sets the correlation ID
     * header on the [Message] to either the existing MDC variable or a new value if not set in MDC.
     * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
     */
    @Before("execution(* org.springframework.messaging.support.AbstractMessageChannel.send(..))")
    // @Before("execution(* io.awspring.cloud.messaging.core.QueueMessageChannel.send(..))")
    fun beforeSendMessage(joinPoint: JoinPoint) {
        val message = joinPoint.args[0] as Message<*>?
        message?.headers?.put(CORRELATION_ID_HEADER, getCurrentCorrelationId())
    }
}

/**
 * AOP Aspect for Scheduled tasks (ie. cron tasks) that sets the correlation ID MDC variable.
 * Due to the invocation semantics of a Scheduled task it does not make sense to pass a correlation ID from another
 * system or process into it.
 */
@Aspect
@Component
class CorrelationIdMdcScheduledAspect {

    @Before("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    fun before(joinPoint: JoinPoint) {
        MDC.put(CORRELATION_ID, generateCorrelationId())
    }
}

private fun generateCorrelationId(): String =
    UUID.randomUUID().toString().replace("-", "")

private fun getCurrentCorrelationId() : String =
    MDC.get(CORRELATION_ID_HEADER) ?: generateCorrelationId()