package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.config.SftpProperties
import uk.gov.dluhc.printapi.messaging.models.ProcessPrintResponseFileMessage
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseFileReadinessService(
    private val sftpService: SftpService,
    private val sftpProperties: SftpProperties,
    private val printMessagingService: PrintMessagingService
) {

    /**
     * This method renames all the unprocessed files within the directory defined by [`sftpProperties.printResponseDownloadDirectory`] and
     * sends a SQS message to the downstream queue to process each of the unprocessed renamed file.
     * If there is any exception while renaming the file, then the process is fail-safe as it continues with the next unprocessed file.
     */
    fun markAndSubmitPrintResponseFileForProcessing() {
        val outboundFolderPath = sftpProperties.printResponseDownloadDirectory
        logger.info { "Finding matching print responses from directory: [$outboundFolderPath]" }

        with(sftpService.identifyFilesToBeProcessed(outboundFolderPath)) {
            logger.info { "Found [$size] unprocessed print responses" }
            forEachIndexed { index, unprocessedFileName ->
                try {
                    sftpService.markFileForProcessing(
                        directory = outboundFolderPath,
                        originalFileName = unprocessedFileName
                    ).also {
                        val messagePayload = ProcessPrintResponseFileMessage(outboundFolderPath, it)
                        logger.info { "Submitting SQS message for file: [${index + 1} of $size] with payload: $messagePayload" }
                        printMessagingService.submitPrintResponseFileForProcessing(messagePayload)
                    }
                } catch (e: IOException) {
                    logger.warn { "Error renaming [$unprocessedFileName] due to error: [${e.cause?.message ?: e.cause}]. Processing will continue for rest of the files" }
                }
            }
        }
        logger.info { "Completed marking and processing all print response files from directory: [$outboundFolderPath]" }
    }
}
