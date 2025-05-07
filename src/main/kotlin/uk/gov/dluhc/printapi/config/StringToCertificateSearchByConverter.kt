package uk.gov.dluhc.printapi.config

import mu.KotlinLogging
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.models.CertificateSearchBy

private val logger = KotlinLogging.logger {}

/**
 * [Converter] that converts a String to [CertificateSearchBy], supporting Spring binding of web request parameters
 * to [CertificateSearchBy] variables
 */
@Component
class StringToCertificateSearchByConverter : Converter<String, CertificateSearchBy> {
    override fun convert(source: String): CertificateSearchBy? =
        CertificateSearchBy.values()
            .firstOrNull { it.value == source }
            .also {
                if (it == null) {
                    logger.info { "No SearchBy found for value $source" }
                }
            }
}
