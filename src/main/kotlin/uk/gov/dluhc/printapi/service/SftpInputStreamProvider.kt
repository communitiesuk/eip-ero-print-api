package uk.gov.dluhc.printapi.service

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

@Service
class SftpInputStreamProvider(
    private val printRequestsFileProducer: PrintRequestsFileProducer,
    private val s3Client: S3Client,
    private val taskExecutor: ThreadPoolTaskExecutor
) {

    /**
     * Creates an SFTP Input Stream for the provided file contents.
     * A Zip output stream is created in a separate thread and piped to
     * the SFTP input stream.
     */
    fun createSftpInputStream(fileDetails: FileDetails): InputStream {
        val zipOutputStream = PipedOutputStream()
        val sftpInputStream = PipedInputStream(zipOutputStream)
        taskExecutor.execute(
            ZipOutputStreamProducerRunnable(
                s3Client,
                zipOutputStream,
                sftpInputStream,
                fileDetails,
                printRequestsFileProducer,
            )
        )
        return sftpInputStream
    }
}
