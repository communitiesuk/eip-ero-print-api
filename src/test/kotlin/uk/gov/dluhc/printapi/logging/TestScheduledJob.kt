package uk.gov.dluhc.printapi.logging

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class TestScheduledJob {

    @Scheduled(cron = Scheduled.CRON_DISABLED)
    fun run() {
        logger.info { "Test scheduled job successfully called" }
    }
}
