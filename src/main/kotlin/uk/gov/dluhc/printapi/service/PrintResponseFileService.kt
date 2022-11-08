package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseFileService(
    val sftpService: SftpService,
) {
    fun processPrintResponseFile(directory: String, fileName: String) {
        val printResponsesString = sftpService.fetchFileFromOutBoundDirectory(directory, fileName)
        processPrintResponseContent(printResponsesString)
        sftpService.removeFileFromOutBoundDirectory(directory, fileName)
    }

    private fun processPrintResponseContent(printResponsesString: String) {
        logger.info { "processing $printResponsesString" }
        // TODO in EIP1-2262
    }
}
