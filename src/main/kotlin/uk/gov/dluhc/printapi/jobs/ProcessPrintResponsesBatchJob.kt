package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.service.PrintResponseFileReadinessService

private val logger = KotlinLogging.logger {}

@Component
class ProcessPrintResponsesBatchJob(
    private val printResponseFileReadinessService: PrintResponseFileReadinessService
) {

    @Scheduled(cron = "\${jobs.process-print-responses.cron}")
    @SchedulerLock(name = "\${jobs.process-print-responses.name}")
    fun pollAndProcessPrintResponses() {
        // Crude way to disable the job, temp for DRT
        if (1 == 0) {
            printResponseFileReadinessService.markAndSubmitPrintResponseFileForProcessing()
        }
        logger.info { "Process print responses job run (disabled in DRT)" }
    }
}
