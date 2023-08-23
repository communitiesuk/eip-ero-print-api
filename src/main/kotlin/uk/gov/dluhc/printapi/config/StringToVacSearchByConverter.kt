package uk.gov.dluhc.printapi.config

import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.models.VacSearchBy

private val logger = KotlinLogging.logger {}

/**
 * [Converter] that converts a String to [VacSearchBy], supporting Spring binding of web request parameters
 * to [VacSearchBy] variables
 */
@Component
class StringToVacSearchByConverter : Converter<String, VacSearchBy> {
    override fun convert(source: String): VacSearchBy? =
        VacSearchBy.values()
            .firstOrNull { it.value == source }
            .also {
                if (it == null) {
                    logger.info { "No SearchBy found for value $source" }
                }
            }
}
