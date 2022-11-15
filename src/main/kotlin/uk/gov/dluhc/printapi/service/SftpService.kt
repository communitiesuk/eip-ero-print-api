package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.integration.file.FileHeaders
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.support.MessageBuilder
import org.springframework.messaging.MessagingException
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

@Service
class SftpService(
    @Qualifier("sftpInboundTemplate") private val sftpInboundTemplate: SftpRemoteFileTemplate,
    @Qualifier("sftpOutboundTemplate") private val sftpOutboundTemplate: SftpRemoteFileTemplate,
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
     * Returns list of all files present in the directory
     * @param filesDirectoryPath the location of the status files
     */
    fun identifyFilesToBeProcessed(filesDirectoryPath: String): List<String> =
        sftpOutboundTemplate
            .list(filesDirectoryPath)
            .map { it.filename }
            .filterNot { it.endsWith(PROCESSING_SUFFIX) }
            .filter { it.matches(STATUS_RESPONSE_FILE_REGEX) }

    /**
     * Renames the file by suffixing ".processing" to its original name. Returns the newly renamed file name
     * Note: The "/" is appended between fileDirectoryPath and fileName before it's renamed
     * @param directory the location of the status file to be renamed
     * @param originalFileName the name of the file e.g. fileName.json
     * @throws IOException while renaming the file.
     */
    @Throws(IOException::class)
    fun markFileForProcessing(directory: String, originalFileName: String): String {
        val newFileName = "$originalFileName$PROCESSING_SUFFIX"
        logger.info { "Renaming [$originalFileName] to [$newFileName] in directory:[$directory]" }
        sftpOutboundTemplate.rename(
            createFileNamePath(directory, originalFileName),
            createFileNamePath(directory, newFileName)
        )
        return newFileName
    }

    /**
     * Fetches the file from the path on the remote server
     * @param directory the path to the file on the remote server
     * @param fileName the path to the file on the remote server
     * @return the contents of the remote file
     */
    fun fetchFileFromOutBoundDirectory(directory: String, fileName: String): String {
        var responseString: String? = null
        sftpOutboundTemplate.get(createFileNamePath(directory, fileName)) {
            responseString = IOUtils.toString(it, StandardCharsets.UTF_8)
        }
        return responseString!!
    }

    @Throws(IOException::class)
    fun removeFileFromOutBoundDirectory(directory: String, fileName: String): Boolean {
        logger.info { "Removing processed file [$fileName] from directory [$directory]" }
        try {
            return sftpOutboundTemplate.remove(createFileNamePath(directory, fileName))
        } catch (ex: MessagingException) {
            throw IOException(ex)
        }
    }

    private fun createFileNamePath(fileDirectory: String, fileName: String) = "$fileDirectory/$fileName"
}
