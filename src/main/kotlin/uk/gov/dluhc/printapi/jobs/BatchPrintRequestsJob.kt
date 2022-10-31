package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class BatchPrintRequestsJob {

    @Scheduled(cron = "\${jobs.print-requests.cron}")
//    @SchedulerLock(name = "\${jobs.print-requests.name}")
    fun run() {
        logger.info { "Hello, I'm your batching agent" }
    }
}
