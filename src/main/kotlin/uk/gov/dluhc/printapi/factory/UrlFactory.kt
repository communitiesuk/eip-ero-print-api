package uk.gov.dluhc.printapi.factory

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.dluhc.printapi.dto.SourceType
import uk.gov.dluhc.printapi.dto.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.dto.SourceType.VOTER_CARD

/**
 * Factory for creating absolute URLs that are configurable for AWS API Gateways.
 */
@Component
class UrlFactory(
    @Value("\${api.print-api.base.url}") private val printApiBaseUrl: String
) {
    fun createPhotoUrl(eroId: String, sourceType: SourceType, applicationId: String): String {
        return when (sourceType) {
            ANONYMOUS_ELECTOR_DOCUMENT -> "$printApiBaseUrl/eros/$eroId/anonymous-elector-documents/photo?applicationId=$applicationId"
            VOTER_CARD -> throw UnsupportedOperationException("print-api does not currently support returning the URL of VAC or Temporary Certificate photos")
        }
    }
}
