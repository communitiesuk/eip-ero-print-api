package uk.gov.dluhc.printapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.SftpException
import mu.KotlinLogging
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import java.io.IOException

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseFileService(
    val sftpService: SftpService,
    val objectMapper: ObjectMapper,
    val printResponseProcessingService: PrintResponseProcessingService
) {
    fun processPrintResponseFile(directory: String, fileName: String) {
        val printResponsesString = sftpService.fetchFileFromOutBoundDirectory(directory, fileName)
        val printResponses = parsePrintResponseContent(printResponsesString)
        printResponseProcessingService.processBatchAndPrintResponses(printResponses)
        removeFile(directory, fileName)
    }

    private fun parsePrintResponseContent(printResponsesString: String): PrintResponses {
        logger.debug { "Parsing print responses $printResponsesString" }
        return objectMapper.readValue(printResponsesString, PrintResponses::class.java)
    }

    private fun removeFile(directory: String, fileName: String) {
        try {
            sftpService.removeFileFromOutBoundDirectory(directory, fileName)
        } catch (ex: IOException) {
            if (isCauseNoSuchFile(ex)) {
                logger.warn("File $fileName was not found when trying to remove from the directory $directory")
            } else {
                logger.error("An error occurred when trying to remove file $fileName from the directory $directory. ${ex.cause?.message}")
            }
        }
    }

    private fun isCauseNoSuchFile(ex: IOException): Boolean {
        if (ex.cause !is MessagingException)
            return false

        val innerCause = (ex.cause as MessagingException).cause?.cause
        return innerCause is SftpException && innerCause.id == ChannelSftp.SSH_FX_NO_SUCH_FILE
    }
}
