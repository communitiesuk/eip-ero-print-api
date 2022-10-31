package uk.gov.dluhc.printapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.integration.annotation.IntegrationComponentScan
import org.springframework.integration.config.EnableIntegration

/**
 * Spring Boot application bootstrapping class.
 */
@SpringBootApplication
@IntegrationComponentScan
@EnableIntegration
@ConfigurationPropertiesScan
class PrintApiApplication

fun main(args: Array<String>) {
    runApplication<PrintApiApplication>(*args)
}
