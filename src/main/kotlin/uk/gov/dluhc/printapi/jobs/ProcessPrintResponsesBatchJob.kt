package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.config.SftpProperties
import uk.gov.dluhc.printapi.messaging.MessageQueue
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import uk.gov.dluhc.printapi.service.SftpService
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private val logger = KotlinLogging.logger {}

@Component
class ProcessPrintResponsesBatchJob(
    private val sftpService: SftpService,
    private val sftpProperties: SftpProperties,
    private val processPrintResponseFileQueue: MessageQueue<ProcessPrintResponseFileMessage>,
    private val clock: Clock
) {

    @Scheduled(cron = "\${jobs.process-print-responses.cron}")
    @SchedulerLock(name = "\${jobs.process-print-responses.name}")
    fun pollAndProcessPrintResponses() {
        val outboundFolderPath = sftpProperties.printResponseDownloadDirectory
        val jobStartTime = dateTimeAsOfNowInMillis()

        logger.info { "Polling for print responses at [$jobStartTime] from directory: [$outboundFolderPath]" }

        val unprocessedFiles = sftpService.identifyFilesToBeProcessed(outboundFolderPath)
        logger.info { "Found [${unprocessedFiles.size}] unprocessed print responses" }

        unprocessedFiles.forEachIndexed { index, unprocessedFile ->
            sftpService.markFileForProcessing(
                directory = outboundFolderPath,
                originalFileName = unprocessedFile.filename
            ).also {
                val messagePayload = ProcessPrintResponseFileMessage(outboundFolderPath, it)
                logger.info { "Sending SQS message for file: [${index + 1} of ${unprocessedFiles.size}] with payload: $messagePayload" }
                processPrintResponseFileQueue.submit(messagePayload)
            }
        }

        val jobCompletionTime = dateTimeAsOfNowInMillis()
        logger.info { "Completed print response processing job at [$jobCompletionTime] from directory: [$outboundFolderPath] in [${Duration.between(jobStartTime, jobCompletionTime)}]" }
    }

    private fun dateTimeAsOfNowInMillis() =
        LocalDateTime.now(clock).toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
}
