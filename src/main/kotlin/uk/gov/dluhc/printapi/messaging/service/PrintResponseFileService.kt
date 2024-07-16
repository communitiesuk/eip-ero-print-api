package uk.gov.dluhc.printapi.messaging.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.printprovider.models.PrintResponses
import uk.gov.dluhc.printapi.service.SftpService
import uk.gov.dluhc.printapi.service.StatisticsUpdateService

private val logger = KotlinLogging.logger {}

@Service
class PrintResponseFileService(
    val sftpService: SftpService,
    val objectMapper: ObjectMapper,
    val printResponseProcessingService: PrintResponseProcessingService,
    val statisticsUpdateService: StatisticsUpdateService,
) {
    fun processPrintResponseFile(directory: String, fileName: String) {
        val printResponsesString = sftpService.fetchFileFromOutBoundDirectory(directory, fileName)
        val printResponses = parsePrintResponseContent(printResponsesString)
        val certificates = printResponseProcessingService.processBatchResponses(printResponses.batchResponses)
        printResponseProcessingService.processPrintResponses(printResponses.printResponses)
        removeFile(directory, fileName)

        certificates.forEach {
            statisticsUpdateService.triggerVoterCardStatisticsUpdate(it.sourceReference!!)
        }
    }

    private fun parsePrintResponseContent(printResponsesString: String): PrintResponses {
        logger.debug { "Parsing print responses $printResponsesString" }
        return objectMapper.readValue(printResponsesString, PrintResponses::class.java)
    }

    private fun removeFile(directory: String, fileName: String) {
        try {
            if (!sftpService.removeFileFromOutBoundDirectory(directory, fileName)) {
                logger.warn("File $fileName was not found when trying to remove from the directory $directory")
            }
        } catch (ex: Exception) {
            logger.error("An error occurred when trying to remove file $fileName from the directory $directory. ${ex.message}")
        }
    }
}
