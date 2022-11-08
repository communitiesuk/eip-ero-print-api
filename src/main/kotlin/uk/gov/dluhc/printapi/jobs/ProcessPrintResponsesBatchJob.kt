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
        val pollingStartTime = dateTimeAsOfNowInMillis()

        logger.info { "Polling print response at [$pollingStartTime] from directory: [$outboundFolderPath]" }

        sftpService.identifyFilesToBeProcessed(outboundFolderPath)
            .forEachIndexed { index, unprocessedFile ->
                sftpService.markFileForProcessing(
                    directory = outboundFolderPath,
                    originalFileName = unprocessedFile.filename
                ).also {
                    val messagePayload = ProcessPrintResponseFileMessage(outboundFolderPath, it)
                    logger.info { "Sending SQS message for recordNo: [$index] with payload: $messagePayload" }
                    processPrintResponseFileQueue.submit(messagePayload)
                }
            }

        val pollingEndTime = dateTimeAsOfNowInMillis()
        logger.info { "Completed print response processing at [$pollingEndTime}] from directory: [$outboundFolderPath] in [${Duration.between(pollingStartTime, pollingEndTime)}]" }
    }

    private fun dateTimeAsOfNowInMillis() =
        LocalDateTime.now(clock).toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS)
}
