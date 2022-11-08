package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.config.SftpProperties
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseFileReadinessService(
    private val sftpService: SftpService,
    private val sftpProperties: SftpProperties,
    private val printMessagingService: PrintMessagingService
) {

    fun markPrintResponseFileForProcessing() {
        val stopWatch = StopWatch.createStarted()
        val outboundFolderPath = sftpProperties.printResponseDownloadDirectory
        logger.info { "Finding matching print responses from directory: [$outboundFolderPath]" }

        val unprocessedFiles = sftpService.identifyFilesToBeProcessed(outboundFolderPath)
        logger.info { "Found [${unprocessedFiles.size}] unprocessed print responses" }

        val filesFailedToProcess = mutableListOf<String>()
        unprocessedFiles.forEachIndexed { index, unprocessedFileName ->
            try {
                sftpService.markFileForProcessing(
                    directory = outboundFolderPath,
                    originalFileName = unprocessedFileName
                ).also {
                    val messagePayload = ProcessPrintResponseFileMessage(outboundFolderPath, it)
                    logger.info { "Submitting SQS message for file: [${index + 1} of ${unprocessedFiles.size}] with payload: $messagePayload" }
                    printMessagingService.submitPrintResponseFileForProcessing(messagePayload)
                }
            } catch (e: RuntimeException) {
                filesFailedToProcess.add(unprocessedFileName)
                logger.warn { "Error renaming [$unprocessedFileName] due to [${e.message}]. Processing will continue for rest of the files" }
            }
        }

        stopWatch.stop()
        logger.warn { "${filesFailedToProcess.size} files failures that were not processed $filesFailedToProcess" }
        logger.info { "Completed marking and processing all print response files in [$stopWatch]" }
    }
}
