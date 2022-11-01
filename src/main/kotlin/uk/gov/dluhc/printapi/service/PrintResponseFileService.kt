package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseFileService(
    val sftpService: SftpService,
) {
    fun fetchAndUnmarshallPrintResponses(filePathToProcess: String): PrintResponses =
        sftpService.fetchAndUnmarshallFile(filePathToProcess, PrintResponses::class.java)

    fun processPrintResponses(response: PrintResponses) {
        logger.info { "processing $response" }
        // TODO in subsequent PR
    }

    fun removeRemoteFile(filePathToProcess: String) =
        sftpService.removeFileFromOutBoundDirectory(filePathToProcess)
}
