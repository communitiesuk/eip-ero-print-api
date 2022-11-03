package uk.gov.dluhc.printapi.jobs

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.config.SftpProperties
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintBatchStatusUpdateMessage
import uk.gov.dluhc.printapi.service.SftpService
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

@Component
class ProcessPrintResponsesBatchJob(
    private val sftpService: SftpService,
    private val sftpProperties: SftpProperties,
) {

    @Scheduled(cron = "\${jobs.process-print-responses.cron}")
    @SchedulerLock(name = "\${jobs.process-print-responses.name}")
    fun pollAndProcessPrintResponses() {
        val outboundFolderPath = sftpProperties.printResponseDownloadDirectory
        logger.info { "Polling print response at [${LocalDateTime.now()}] from directory:[$outboundFolderPath]" }

        sftpService.identifyFilesToBeProcessed(outboundFolderPath)
            .forEach { unprocessedFile ->
                sftpService.markFileForProcessing(
                    fileDirectoryPath = outboundFolderPath,
                    originalFileName = unprocessedFile.filename
                ).also {
                    // TODO EIP1-2261 will send a SQS message to print-api queue
                    val messagePayload = ProcessPrintBatchStatusUpdateMessage(outboundFolderPath, it)
                    logger.info { "Sending SQS message with payload $messagePayload" }
                }
            }
    }
}
