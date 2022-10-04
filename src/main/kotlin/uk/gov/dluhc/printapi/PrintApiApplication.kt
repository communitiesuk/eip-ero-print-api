package uk.gov.dluhc.printapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Spring Boot application bootstrapping class.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
class PrintApiApplication

fun main(args: Array<String>) {
    runApplication<PrintApiApplication>(*args)
}
