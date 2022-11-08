package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.apache.commons.lang3.time.StopWatch
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.config.SftpProperties
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.service.PrintMessagingService
import uk.gov.dluhc.printapi.service.SftpService

private val logger = KotlinLogging.logger {}

@Component
class ProcessPrintResponsesBatchJob(
    private val sftpService: SftpService,
    private val sftpProperties: SftpProperties,
    private val printMessagingService: PrintMessagingService
) {

    @Scheduled(cron = "\${jobs.process-print-responses.cron}")
    @SchedulerLock(name = "\${jobs.process-print-responses.name}")
    fun pollAndProcessPrintResponses() {
        val outboundFolderPath = sftpProperties.printResponseDownloadDirectory
        logger.info { "Polling for print responses from directory: [$outboundFolderPath]" }

        val stopWatch = StopWatch.createStarted()
        val unprocessedFiles = sftpService.identifyFilesToBeProcessed(outboundFolderPath)
        logger.info { "Found [${unprocessedFiles.size}] unprocessed print responses" }

        unprocessedFiles.forEachIndexed { index, unprocessedFile ->
            sftpService.markFileForProcessing(
                directory = outboundFolderPath,
                originalFileName = unprocessedFile.filename
            ).also {
                val messagePayload = ProcessPrintResponseFileMessage(outboundFolderPath, it)
                logger.info { "Submitting SQS message for file: [${index + 1} of ${unprocessedFiles.size}] with payload: $messagePayload" }
                printMessagingService.submitPrintResponseFileForProcessing(messagePayload)
            }
        }

        stopWatch.stop()
        logger.info { "Completed print response processing job from directory: [$outboundFolderPath] in [$stopWatch]" }
    }
}
