package uk.gov.dluhc.printapi.config.aspect

import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private const val WITH_SCHEDULED = "@annotation(org.springframework.scheduling.annotation.Scheduled)"

private val logger = KotlinLogging.logger {}

@Aspect
@Component
class ScheduledJobAspect(
    @Value("\${alarm-magic-strings.scheduled-job}")
    val alarmString: String,
) {

    @Around(WITH_SCHEDULED)
    fun triggerAlarmAndHandleException(joinPoint: ProceedingJoinPoint) {
        try {
            joinPoint.proceed()
        } catch (exception: Exception) {
            val jobName = joinPoint.signature.name
            logger.error { "$alarmString [$jobName]" }
            throw exception
        }
    }
}
