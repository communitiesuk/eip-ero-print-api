package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.Scheduled.CRON_DISABLED
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.service.PrintRequestsService

@Component
class BatchPrintRequestsJob(
    private val printRequestsService: PrintRequestsService,
) {

    @Scheduled(cron = CRON_DISABLED)
    @SchedulerLock(name = "\${jobs.batch-print-requests.name}")
    fun run() {
        printRequestsService.processPrintRequests()
    }
}
