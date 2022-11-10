package uk.gov.dluhc.printapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses

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
        sftpService.removeFileFromOutBoundDirectory(directory, fileName)
    }

    private fun parsePrintResponseContent(printResponsesString: String): PrintResponses {
        logger.debug { "Parsing print responses $printResponsesString" }
        return objectMapper.readValue(printResponsesString, PrintResponses::class.java)
    }
}
