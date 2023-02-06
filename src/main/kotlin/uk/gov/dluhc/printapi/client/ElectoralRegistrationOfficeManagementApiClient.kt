package uk.gov.dluhc.printapi.client

import mu.KotlinLogging
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficesResponse
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.mapper.EroDtoMapper

private val logger = KotlinLogging.logger {}

/**
 * Client class for interacting with the REST API `ero-management-api`
 */
@Component
class ElectoralRegistrationOfficeManagementApiClient(
    private val eroManagementWebClient: WebClient,
    private val eroMapper: EroDtoMapper
) {

    /**
     * Calls the `ero-management-api` to return a [List] of GSS Codes for the specified eroId.
     *
     * @param eroId the ID of the ERO to return
     * @return a [List]<String> containing the GSS Codes associated with the ERO
     * @throws [ElectoralRegistrationOfficeManagementApiException] concrete implementation if the API returns an error
     */
    fun getElectoralRegistrationOfficeGssCodes(eroId: String): List<String> {
        val response = eroManagementWebClient
            .get()
            .uri("/eros/{eroId}", eroId)
            .retrieve()
            .bodyToMono(ElectoralRegistrationOfficeResponse::class.java)
            .onErrorResume { ex -> handleException(ex, mapOf("eroId" to eroId)) }
            .block()!!

        return response.localAuthorities.map { it.gssCode }
    }

    /**
     * Calls the `ero-management-api` for the specified gssCode and maps to an EroDto.
     *
     * @param gssCode the gssCode of the localAuthority for the ERO to be returned
     * @return an [EroDto] for the ERO and Local Authority
     * @throws [ElectoralRegistrationOfficeManagementApiException] concrete implementation if the API returns an error
     */
    fun getEro(gssCode: String): EroDto {
        val response = eroManagementWebClient
            .get()
            .uri("/eros?gssCode=$gssCode")
            .retrieve()
            .bodyToMono(ElectoralRegistrationOfficesResponse::class.java)
            .onErrorResume { ex -> handleException(ex, mapOf("gssCode" to gssCode)) }
            .block()!!

        if (response.eros.size != 1 ||
            response.eros[0].localAuthorities.filter { it.gssCode == gssCode }.size != 1
        ) {
            throw ElectoralRegistrationOfficeNotFoundException(mapOf("gssCode" to gssCode))
        }

        return eroMapper.toEroDto(
            response.eros[0].id,
            response.eros[0].localAuthorities.filter { it.gssCode == gssCode }[0]
        )
    }

    private fun <T> handleException(ex: Throwable, searchCriteria: Map<String, String>): Mono<T> =
        if (ex is WebClientResponseException) {
            handleWebClientResponseException(ex, searchCriteria)
        } else {
            logger.error(ex) { "Unhandled exception thrown by WebClient" }
            Mono.error(ElectoralRegistrationOfficeGeneralException(ex.message, searchCriteria))
        }

    private fun <T> handleWebClientResponseException(
        ex: WebClientResponseException,
        searchCriteria: Map<String, String>
    ): Mono<T> =
        if (ex.statusCode == NOT_FOUND)
            Mono.error(ElectoralRegistrationOfficeNotFoundException(searchCriteria))
        else
            Mono.error(ElectoralRegistrationOfficeGeneralException(ex.message, searchCriteria))
}
