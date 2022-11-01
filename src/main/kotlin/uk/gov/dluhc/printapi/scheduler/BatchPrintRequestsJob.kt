package uk.gov.dluhc.printapi.scheduler

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.service.PrintRequestsService

private val logger = KotlinLogging.logger {}

@Component
class BatchPrintRequestsJob(
    private val printRequestsService: PrintRequestsService,
    @Value("\${jobs.batch-print-requests.batch-size}")
    private val batchSize: Int
) {

    @Scheduled(cron = "\${jobs.batch-print-requests.cron}")
    @SchedulerLock(name = "\${jobs.batch-print-requests.name}")
    fun run() {
        printRequestsService.processPrintRequests(batchSize)
    }
}
