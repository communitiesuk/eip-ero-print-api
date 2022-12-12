package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.service.PrintRequestsService

private val logger = KotlinLogging.logger {}

@Component
class BatchPrintRequestsJob(
    private val printRequestsService: PrintRequestsService,
) {

    @Scheduled(cron = "\${jobs.batch-print-requests.cron}")
    @SchedulerLock(name = "\${jobs.batch-print-requests.name}")
    fun run() {
        printRequestsService.processPrintRequests()
    }
}
