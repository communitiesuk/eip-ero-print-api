package uk.gov.dluhc.printapi.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficesResponse
import uk.gov.dluhc.printapi.mapper.EroDtoMapper
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse

internal class ElectoralRegistrationOfficeManagementApiClientTest {

    private val exchangeFunction: ExchangeFunction = mock()

    private val clientResponse: ClientResponse = mock()

    private val clientRequest = ArgumentCaptor.forClass(ClientRequest::class.java)

    private val eroMapper: EroDtoMapper = mock()

    private val webClient = WebClient.builder()
        .baseUrl("http://ero-management-api")
        .exchangeFunction(exchangeFunction)
        .build()

    private val apiClient = ElectoralRegistrationOfficeManagementApiClient(webClient, eroMapper)

    @BeforeEach
    fun setupWebClientRequestCapture() {
        given(exchangeFunction.exchange(clientRequest.capture())).willReturn(Mono.just(clientResponse))
    }

    @Nested
    inner class GetElectoralRegistrationOffice {
        @Test
        fun `should get Electoral Registration Office`() {
            // Given
            val eroId = aValidRandomEroId()
            val gssCode1 = getRandomGssCode()
            val gssCode2 = getRandomGssCode()
            val eroResponse = buildElectoralRegistrationOfficeResponse(
                localAuthorities = mutableListOf(
                    buildLocalAuthorityResponse(gssCode = gssCode1),
                    buildLocalAuthorityResponse(gssCode = gssCode2),
                )
            )
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
                Mono.just(eroResponse)
            )
            val expected = mutableListOf(gssCode1, gssCode2)

            // When
            val ero = apiClient.getElectoralRegistrationOfficeGssCodes(eroId)

            // Then
            assertThat(ero).isEqualTo(expected)
            assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
        }

        @Test
        fun `should not get Electoral Registration Office given API returns a 404 error`() {
            // Given
            val eroId = aValidRandomEroId()

            val http404Error = NOT_FOUND.toWebClientResponseException()
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
                Mono.error(http404Error)
            )

            val expectedException = ElectoralRegistrationOfficeNotFoundException(mapOf("eroId" to eroId))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getElectoralRegistrationOfficeGssCodes(eroId) },
                ElectoralRegistrationOfficeNotFoundException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expectedException.message)
            assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
        }

        @Test
        fun `should not get Electoral Registration Office given API returns a 500 error`() {
            // Given
            val eroId = aValidRandomEroId()

            val http500Error = INTERNAL_SERVER_ERROR.toWebClientResponseException()
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
                Mono.error(http500Error)
            )

            val expectedException =
                ElectoralRegistrationOfficeGeneralException("500 INTERNAL_SERVER_ERROR", mapOf("eroId" to eroId))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getElectoralRegistrationOfficeGssCodes(eroId) },
                ElectoralRegistrationOfficeGeneralException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expectedException.message)
            assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
        }

        @Test
        fun `should throw exception given API returns a non WebClientResponseException`() {
            // Given
            val eroId = aValidRandomEroId()

            val exception = object : WebClientException("general exception") {}
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java)).willReturn(
                Mono.error(exception)
            )

            val expectedException =
                ElectoralRegistrationOfficeGeneralException("general exception", mapOf("eroId" to eroId))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getElectoralRegistrationOfficeGssCodes(eroId) },
                ElectoralRegistrationOfficeGeneralException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expectedException.message)
            assertRequestByEroIdUri(eroId)
        }
    }

    @Nested
    inner class GetEro {
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
            val expected = buildEroDto()
            given(eroMapper.toEroDto(any())).willReturn(expected)

            // When
            val ero = apiClient.getEro(gssCode)

            // Then
            assertThat(ero).isSameAs(expected)
            assertRequestUri(gssCode)
            verify(eroMapper).toEroDto(eroResponse.localAuthorities[0])
        }

        @Test
        fun `should throw exception given ero does not exist for the gssCode`() {
            // Given
            val gssCode = getRandomGssCode()
            val emptyResponse = ElectoralRegistrationOfficesResponse(emptyList())
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
                Mono.just(emptyResponse)
            )
            val expectedException = ElectoralRegistrationOfficeNotFoundException(mapOf("gssCode" to gssCode))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getEro(gssCode) },
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

            val expectedException = ElectoralRegistrationOfficeNotFoundException(mapOf("gssCode" to gssCode))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getEro(gssCode) },
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
                ElectoralRegistrationOfficeGeneralException("500 INTERNAL_SERVER_ERROR", mapOf("gssCode" to gssCode))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getEro(gssCode) },
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
                ElectoralRegistrationOfficeGeneralException("general exception", mapOf("gssCode" to gssCode))

            // When
            val ex = catchThrowableOfType(
                { apiClient.getEro(gssCode) },
                ElectoralRegistrationOfficeGeneralException::class.java
            )

            // Then
            assertThat(ex.message).isEqualTo(expectedException.message)
            assertRequestUri(gssCode)
            verifyNoInteractions(eroMapper)
        }
    }

    private fun assertRequestUri(gssCode: String) {
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros")
            .hasQuery("gssCode=$gssCode")
    }

    private fun assertRequestByEroIdUri(eroId: String) {
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros/$eroId")
    }
}

private fun HttpStatus.toWebClientResponseException(): WebClientResponseException =
    WebClientResponseException.create(this.value(), this.name, HttpHeaders.EMPTY, "".toByteArray(), null)
