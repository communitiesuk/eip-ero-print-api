package uk.gov.dluhc.logging.config

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * A simple Scheduled Job to be used by [CorrelationIdMdcScheduledAspectTest]
 */
@Component
class TestScheduledJob {

    @Scheduled(cron = Scheduled.CRON_DISABLED)
    fun run() {
        logger.info { "Test scheduled job successfully called" }
    }
}
