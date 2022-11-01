package uk.gov.dluhc.printapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.jcraft.jsch.ChannelSftp
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.integration.file.FileHeaders
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.support.MessageBuilder
import org.springframework.stereotype.Service
import java.io.InputStream

private val logger = KotlinLogging.logger {}

@Service
class SftpService(
    @Qualifier("sftpInboundTemplate") private val sftpInboundTemplate: SftpRemoteFileTemplate,
    @Qualifier("sftpOutboundTemplate") private val sftpOutboundTemplate: SftpRemoteFileTemplate,
    val objectMapper: ObjectMapper,
) {
    companion object {
        private const val PROCESSING_SUFFIX = ".processing"
        private val STATUS_RESPONSE_FILE_REGEX = Regex("status-\\d{17}\\.json")
    }

    fun sendFile(inputStream: InputStream, filename: String): String =
        sftpInboundTemplate.send(
            MessageBuilder
                .withPayload(inputStream)
                .setHeader(FileHeaders.FILENAME, filename)
                .build()
        )

    /**
     * Returns list entry for all files present in the directory
     * @param filesDirectoryPath the location of the status files
     */
    fun identifyFilesToBeProcessed(filesDirectoryPath: String): List<ChannelSftp.LsEntry> =
        sftpOutboundTemplate
            .list(filesDirectoryPath)
            .filterNot { lsEntry -> lsEntry.filename.endsWith(PROCESSING_SUFFIX) }
            .filter { lsEntry -> lsEntry.filename.matches(STATUS_RESPONSE_FILE_REGEX) }

    /**
     * Renames the file by suffixing ".processing" to its original name. Returns the newly renamed file name
     * Note: The "/" is appended between fileDirectoryPath and fileName before it's renamed
     * @param fileDirectoryPath the location of the status file to be renamed
     * @param originalFileName the name of the file e.g. fileName.json
     */
    fun markFileForProcessing(fileDirectoryPath: String, originalFileName: String): String {
        val newFileName = "$originalFileName$PROCESSING_SUFFIX"
        logger.info { "Renaming [$originalFileName] to [$newFileName] in directory:[$fileDirectoryPath]" }
        sftpOutboundTemplate.rename("$fileDirectoryPath/$originalFileName", "$fileDirectoryPath/$newFileName")
        return newFileName
    }

    /**
     * Fetches the file from the path on the remote server and
     * unmarshalls the content to an object of type `responseObjectType`
     * @param filePathToProcess the path to the file on the remote server
     * @param responseObjectType the type of the object to marshall the json content of the file to
     */
    fun <T> fetchAndUnmarshallFile(filePathToProcess: String, responseObjectType: Class<T>): T {
        var responseObject: T? = null
        sftpOutboundTemplate.get(filePathToProcess) { responseObject = objectMapper.readValue(it, responseObjectType) }
        return responseObject!!
    }

    fun removeFileFromOutBoundDirectory(filePathToProcess: String) =
        sftpOutboundTemplate.remove(filePathToProcess)
}
