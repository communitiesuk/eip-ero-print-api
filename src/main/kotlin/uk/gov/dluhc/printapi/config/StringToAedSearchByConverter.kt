package uk.gov.dluhc.printapi.config

import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.models.AedSearchBy

private val logger = KotlinLogging.logger {}

/**
 * [Converter] that converts a String to [AedSearchBy], supporting Spring binding of web request parameters
 * to [SearchBy] variables
 */
@Component
class StringToAedSearchByConverter : Converter<String, AedSearchBy> {
    override fun convert(source: String): AedSearchBy? =
        AedSearchBy.values()
            .firstOrNull { it.value == source }
            .also {
                if (it == null) {
                    logger.info { "No SearchBy found for value $source" }
                }
            }
}
