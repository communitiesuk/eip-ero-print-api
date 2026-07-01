package uk.gov.dluhc.printapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.config.EnableIntegration

/**
 * Spring Boot application bootstrapping class.
 */
@SpringBootApplication
@IntegrationComponentScan
@EnableIntegration
@ConfigurationPropertiesScan
@EnableJpaAuditing
class PrintApiApplication

fun main(args: Array<String>) {
    // TODO diagnostic flag to rule out netty's native epoll transport as the cause of
    // intermittent SFTP (Apache MINA SSHD NIO2) session failures seen after the Netty
    // 4.1.135.Final upgrade - remove once root cause is confirmed/fixed.
    System.setProperty("io.netty.transport.noNative", "true")

    runApplication<PrintApiApplication>(*args)
}
