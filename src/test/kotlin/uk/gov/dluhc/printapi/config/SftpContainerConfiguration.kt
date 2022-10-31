package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile
import java.net.URL

@Configuration
class SftpContainerConfiguration {

    companion object {
        const val DEFAULT_SFTP_PORT = 22
        const val USER = "user"
        private const val PUBLIC_KEY_FILENAME = "ssh/printer_rsa.pub"
        const val REMOTE_PATH = "EROP/Dev/OutBound" // must match sftp.print-request-upload-directory
        val DIRECTORIES = listOf("EROP/Dev/InBound", REMOTE_PATH)
        const val USER_ID = 1001
        const val GROUP_ID = 100

        private var container: GenericContainer<*>? = null

        /**
         * Creates and starts container running an SFTP server.
         * Returns the container that can subsequently be used for further setup and configuration.
         */
        fun getInstance(): GenericContainer<*> {
            if (container == null) {
                val publicKeyResourceUrl: URL = getPublicKeyResourceUrl()
                container = GenericContainer(
                    ImageFromDockerfile()
                        .withDockerfileFromBuilder { builder ->
                            builder
                                .from("atmoz/sftp:latest")
                                .run("mkdir -p /home/$USER/$REMOTE_PATH; chmod -R 007 /home/$USER")
                                .build()
                        }
                )
                    .withReuse(true) // NOTE: ImageFromDockerfile does not currently support this feature
                    .withFileSystemBind(publicKeyResourceUrl.file, "/home/$USER/.ssh/keys/id_rsa.pub", BindMode.READ_ONLY)
                    .withExposedPorts(DEFAULT_SFTP_PORT)
                    .withCommand("$USER::$USER_ID:$GROUP_ID:${DIRECTORIES.joinToString(",")}")
                    .apply {
                        start()
                    }
            }

            return container!!
        }

        private fun getPublicKeyResourceUrl(): URL = ClassLoader.getSystemResource(PUBLIC_KEY_FILENAME)
    }
}
