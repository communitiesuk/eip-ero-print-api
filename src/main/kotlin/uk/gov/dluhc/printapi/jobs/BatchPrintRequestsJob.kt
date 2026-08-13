package uk.gov.dluhc.printapi.jobs

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.service.PrintRequestsService

/*
 * Ensure that any changes to the name of this job class "BatchPrintRequestsJob" is reflected in Infra's print_api.tf
 */
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
