package uk.gov.dluhc.printapi.client

import mu.KotlinLogging
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficesResponse
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.mapper.EroManagementApiEroDtoMapper

private val logger = KotlinLogging.logger {}

/**
 * Client class for interacting with the REST API `ero-management-api`
 */
@Component
class ElectoralRegistrationOfficeManagementApiClient(
    private val eroManagementWebClient: WebClient,
    private val eroMapper: EroManagementApiEroDtoMapper
) {

    /**
     * Calls the `ero-management-api` to return a [EroManagementApiEroDto] for the specified gssCode.
     *
     * @param gssCode the gssCode of the localAuthority for the ERO to be returned
     * @return an [EroManagementApiEroDto] for the ERO
     * @throws [ElectoralRegistrationOfficeManagementApiException] concrete implementation if the API returns an error
     */
    fun getElectoralRegistrationOffice(gssCode: String): EroManagementApiEroDto {
        val response = eroManagementWebClient
            .get()
            .uri("/eros?gssCode=$gssCode")
            .retrieve()
            .bodyToMono(ElectoralRegistrationOfficesResponse::class.java)
            .onErrorResume { ex -> handleException(ex, gssCode) }
            .block()!!

        if (response.eros.isEmpty()) {
            throw ElectoralRegistrationOfficeNotFoundException(gssCode)
        }

        return eroMapper.toEroManagementApiEroDto(response.eros[0])
    }

    private fun handleException(ex: Throwable, gssCode: String): Mono<ElectoralRegistrationOfficesResponse> =
        if (ex is WebClientResponseException) {
            handleWebClientResponseException(ex, gssCode)
        } else {
            logger.error(ex) { "Unhandled exception thrown by WebClient" }
            Mono.error(ElectoralRegistrationOfficeGeneralException("Unhandled error getting ERO for gssCode $gssCode"))
        }

    private fun handleWebClientResponseException(
        ex: WebClientResponseException,
        gssCode: String
    ): Mono<ElectoralRegistrationOfficesResponse> =
        if (ex.statusCode == NOT_FOUND)
            Mono.error(ElectoralRegistrationOfficeNotFoundException(gssCode))
        else
            Mono.error(ElectoralRegistrationOfficeGeneralException("Error ${ex.message} getting ERO for gssCode $gssCode"))
}
