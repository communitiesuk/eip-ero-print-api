package uk.gov.dluhc.printapi.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.MDC
import org.springframework.messaging.Message
import org.springframework.messaging.support.GenericMessage
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
     * Around Advice for inbound [Message]s (ie. SQS Message's being directed to a listener class) that sets the correlation ID
     * MDC variable to the value found in the Message header `x-correlation-id` if set, or a new value.
     * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
     */
    @Around("execution(* org.springframework.messaging.handler.invocation.AbstractMethodMessageHandler.handleMessage(..))")
    fun aroundHandleMessage(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        val message = proceedingJoinPoint.args[0] as Message<*>?
        MDC.put(CORRELATION_ID, message?.headers?.get(CORRELATION_ID_HEADER)?.toString() ?: generateCorrelationId())
        return proceedingJoinPoint.proceed(proceedingJoinPoint.args).also {
            MDC.remove(CORRELATION_ID)
        }
    }

    /**
     * Around Advice for outbound [Message]s (ie. SQS Message's being sent) that sets the correlation ID
     * header on a new [Message] to either the existing MDC variable or a new value if not set in MDC.
     * This allows for passing and logging a consistent correlation ID between disparate systems or processes.
     *
     * The reason this Advice is an Around is because [Message] and it's headers are immutable, so we cannot add
     * the correlation ID header on the passed [Message]. Therefore we need to create new message with the same
     * payload and a modified collection of headers.
     */
    @Around("execution(* io.awspring.cloud.messaging.core.support.AbstractMessageChannelMessagingSendingTemplate.send(..))")
    fun aroundSendMessage(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        val queue = proceedingJoinPoint.args[0]
        val originalMessage = proceedingJoinPoint.args[1] as Message<*>
        val newMessage = GenericMessage(
            originalMessage.payload,
            originalMessage.headers.toMutableMap().plus(CORRELATION_ID_HEADER to getCurrentCorrelationId())
        )
        return proceedingJoinPoint.proceed(arrayOf(queue, newMessage))
    }
}

/**
 * AOP Aspect for Scheduled tasks (ie. cron tasks) that sets the correlation ID MDC variable.
 */
@Aspect
@Component
class CorrelationIdMdcScheduledAspect {

    /**
     * Around Advice for Scheduled tasks (ie. cron tasks) that sets the correlation ID MDC variable to a new value.
     * Due to the invocation semantics of a Scheduled task it does not make sense to pass a correlation ID from another
     * system or process into it.
     */
    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    fun aroundScheduledTask(proceedingJoinPoint: ProceedingJoinPoint): Any? {
        MDC.put(CORRELATION_ID, generateCorrelationId())
        return proceedingJoinPoint.proceed(proceedingJoinPoint.args).also {
            MDC.remove(CORRELATION_ID)
        }
    }
}

private fun generateCorrelationId(): String =
    UUID.randomUUID().toString().replace("-", "")

private fun getCurrentCorrelationId(): String =
    MDC.get(CORRELATION_ID_HEADER) ?: generateCorrelationId()
