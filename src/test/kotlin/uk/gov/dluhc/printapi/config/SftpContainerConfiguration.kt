package uk.gov.dluhc.printapi.config

import mu.KotlinLogging
import org.apache.commons.lang3.time.StopWatch
import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

private val logger = KotlinLogging.logger {}

@Configuration
class SftpContainerConfiguration {

    companion object {
        const val DEFAULT_SFTP_PORT = 22
        private const val USER = "user"
        private const val PUBLIC_KEY_FILENAME = "ssh/printer_rsa.pub"
        private const val PUBLIC_KEY_FILENAME_RELATIVE_URL = "src/test/resources/$PUBLIC_KEY_FILENAME"
        const val PRINT_REQUEST_UPLOAD_PATH = "EROP/Dev/InBound" // must match sftp.print-request-upload-directory
        const val PRINT_RESPONSE_DOWNLOAD_PATH = "EROP/Dev/OutBound" // must match sftp.print-response-download-directory
        private val DIRECTORIES = listOf(PRINT_REQUEST_UPLOAD_PATH, PRINT_RESPONSE_DOWNLOAD_PATH)
        private const val USER_ID = 1001
        private const val GROUP_ID = 100

        private var container: GenericContainer<*>? = null

        /**
         * Creates and starts container running an SFTP server.
         * Returns the container that can subsequently be used for further setup and configuration.
         */
        fun getInstance(): GenericContainer<*> {
            if (container == null) {
                val sw = StopWatch.createStarted()

                container = GenericContainer(
                    ImageFromDockerfile()
                        .withDockerfileFromBuilder { builder ->
                            builder
                                .from("atmoz/sftp:latest")
                                .build()
                        }
                )
                    .withReuse(true) // NOTE: ImageFromDockerfile does not currently support this feature
                    .withFileSystemBind(PUBLIC_KEY_FILENAME_RELATIVE_URL, "/home/$USER/.ssh/keys/id_rsa.pub", BindMode.READ_ONLY)
                    .withExposedPorts(DEFAULT_SFTP_PORT)
                    .withCommand("$USER::$USER_ID:$GROUP_ID:${DIRECTORIES.joinToString(",")}")
                    .apply {
                        start()
                        logger.info { "sftp mapped port: ${getMappedPort(22)}" }
                    }
                sw.stop()
                logger.info { "sftp container started in: $sw" }
            }

            return container!!
        }
    }
}
