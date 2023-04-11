package uk.gov.dluhc.logging.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.MDC

/**
 * AOP Aspect for Scheduled tasks (ie. cron tasks) that sets the correlation ID MDC variable.
 */
@Aspect
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
