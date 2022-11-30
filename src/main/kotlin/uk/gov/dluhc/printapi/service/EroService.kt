package uk.gov.dluhc.printapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiException

private val logger = KotlinLogging.logger {}

@Service
class EroService(private val electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient) {

    fun lookupGssCodesForEro(eroId: String): List<String> =
        try {
            electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOfficeGssCodes(eroId)
        } catch (ex: ElectoralRegistrationOfficeManagementApiException) {
            logger.info { "Error ${ex.message} returned whilst looking up the gssCodes for ERO $eroId" }
            throw ex
        }
}
