package uk.gov.dluhc.printapi.config

import org.springframework.context.annotation.Configuration
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

@Configuration
class SftpContainerConfiguration {

    companion object {
        const val DEFAULT_SFTP_PORT = 22
        const val HOST = "localhost"
        const val USER = "user"
        const val PASSWORD = "password"
        const val REMOTE_PATH = "upload" // must match sftp.remote-directory

        private var container: GenericContainer<*>? = null

        /**
         * Creates and starts container running an SFTP server.
         * Returns the container that can subsequently be used for further setup and configuration.
         */
        fun getInstance(): GenericContainer<*> {
            if (container == null) {
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
                    .withExposedPorts(DEFAULT_SFTP_PORT)
                    .withCommand("$USER:$PASSWORD:1001:::$REMOTE_PATH")
                    .apply {
                        start()
                    }
            }

            return container!!
        }
    }
}
