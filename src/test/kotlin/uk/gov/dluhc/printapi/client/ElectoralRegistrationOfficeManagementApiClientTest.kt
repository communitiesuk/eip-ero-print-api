package uk.gov.dluhc.printapi.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficesResponse
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.dto.EroManagementApiLocalAuthorityDto
import uk.gov.dluhc.printapi.mapper.EroManagementApiEroDtoMapper
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse

internal class ElectoralRegistrationOfficeManagementApiClientTest {

    private val exchangeFunction: ExchangeFunction = mock()

    private val clientResponse: ClientResponse = mock()

    private val clientRequest = ArgumentCaptor.forClass(ClientRequest::class.java)

    private val eroMapper: EroManagementApiEroDtoMapper = mock()

    private val webClient = WebClient.builder()
        .baseUrl("http://ero-management-api")
        .exchangeFunction(exchangeFunction)
        .build()

    private val apiClient = ElectoralRegistrationOfficeManagementApiClient(webClient, eroMapper)

    @BeforeEach
    fun setupWebClientRequestCapture() {
        given(exchangeFunction.exchange(clientRequest.capture())).willReturn(Mono.just(clientResponse))
    }

    @Test
    fun `should get Electoral Registration Office given ero exists for the gssCode`() {
        // Given
        val gssCode = getRandomGssCode()
        val eroResponse = buildElectoralRegistrationOfficeResponse(
            localAuthorities = listOf(
                buildLocalAuthorityResponse(gssCode = gssCode),
                buildLocalAuthorityResponse()
            )
        )

        val erosResponse = ElectoralRegistrationOfficesResponse(listOf(eroResponse))
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
            Mono.just(erosResponse)
        )
        val expected = with(eroResponse) {
            EroManagementApiEroDto(
                id = id,
                name = name,
                listOf(
                    EroManagementApiLocalAuthorityDto(
                        gssCode = localAuthorities[0].gssCode!!,
                        name = localAuthorities[0].name!!
                    ),
                    EroManagementApiLocalAuthorityDto(
                        gssCode = localAuthorities[1].gssCode!!,
                        name = localAuthorities[1].name!!
                    ),
                )
            )
        }
        given(eroMapper.toEroManagementApiEroDto(any())).willReturn(expected)

        // When
        val ero = apiClient.getElectoralRegistrationOffice(gssCode)

        // Then
        assertThat(ero).isSameAs(expected)
        assertRequestUri(gssCode)
        verify(eroMapper).toEroManagementApiEroDto(eroResponse)
    }

    @Test
    fun `should throw exception given ero does not exist for the gssCode`() {
        // Given
        val gssCode = getRandomGssCode()
        val emptyResponse = ElectoralRegistrationOfficesResponse(emptyList())
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
            Mono.just(emptyResponse)
        )
        val expectedException = ElectoralRegistrationOfficeNotFoundException(gssCode)

        // When
        val ex = catchThrowableOfType(
            { apiClient.getElectoralRegistrationOffice(gssCode) },
            ElectoralRegistrationOfficeNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertRequestUri(gssCode)
        verifyNoInteractions(eroMapper)
    }

    @Test
    fun `should throw exception given API returns a 404 error`() {
        // Given
        val gssCode = getRandomGssCode()

        val http404Error = NOT_FOUND.toWebClientResponseException()
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
            Mono.error(http404Error)
        )

        val expectedException = ElectoralRegistrationOfficeNotFoundException(gssCode)

        // When
        val ex = catchThrowableOfType(
            { apiClient.getElectoralRegistrationOffice(gssCode) },
            ElectoralRegistrationOfficeNotFoundException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertRequestUri(gssCode)
        verifyNoInteractions(eroMapper)
    }

    @Test
    fun `should throw exception given API returns a 500 error`() {
        // Given
        val gssCode = getRandomGssCode()

        val http500Error = INTERNAL_SERVER_ERROR.toWebClientResponseException()
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
            Mono.error(http500Error)
        )

        val expectedException =
            ElectoralRegistrationOfficeGeneralException("Error 500 INTERNAL_SERVER_ERROR getting ERO for gssCode $gssCode")

        // When
        val ex = catchThrowableOfType(
            { apiClient.getElectoralRegistrationOffice(gssCode) },
            ElectoralRegistrationOfficeGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertRequestUri(gssCode)
        verifyNoInteractions(eroMapper)
    }

    @Test
    fun `should throw exception given API returns a non WebClientResponseException`() {
        // Given
        val gssCode = getRandomGssCode()

        val exception = object : WebClientException("general exception") {}
        given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
            Mono.error(exception)
        )

        val expectedException =
            ElectoralRegistrationOfficeGeneralException("Unhandled error getting ERO for gssCode $gssCode")

        // When
        val ex = catchThrowableOfType(
            { apiClient.getElectoralRegistrationOffice(gssCode) },
            ElectoralRegistrationOfficeGeneralException::class.java
        )

        // Then
        assertThat(ex.message).isEqualTo(expectedException.message)
        assertRequestUri(gssCode)
        verifyNoInteractions(eroMapper)
    }

    private fun assertRequestUri(gssCode: String) {
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros")
            .hasQuery("gssCode=$gssCode")
    }
}

private fun HttpStatus.toWebClientResponseException(): WebClientResponseException =
    WebClientResponseException.create(this.value(), this.name, HttpHeaders.EMPTY, "".toByteArray(), null)
