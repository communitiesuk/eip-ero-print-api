package uk.gov.dluhc.printapi.service

import org.springframework.integration.file.FileHeaders
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate
import org.springframework.integration.support.MessageBuilder
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class SftpService(private val sftpTemplate: SftpRemoteFileTemplate) {

    fun sendFile(inputStream: InputStream, filename: String): String =
        sftpTemplate.send(
            MessageBuilder
                .withPayload(inputStream)
                .setHeader(FileHeaders.FILENAME, filename)
                .build()
        )
}
