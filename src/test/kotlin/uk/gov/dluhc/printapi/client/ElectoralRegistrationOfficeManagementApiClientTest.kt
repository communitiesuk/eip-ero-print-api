package uk.gov.dluhc.printapi.client

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientException
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
            given(clientResponse.statusCode()).willReturn(HttpStatus.OK)
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficeResponse::class.java))
                .willReturn(Mono.just(eroResponse))

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
            given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(NOT_FOUND).build())
            )

            // When
            val ex = catchThrowable { apiClient.getElectoralRegistrationOfficeGssCodes(aValidRandomEroId()) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeNotFoundException::class.java)
        }

        @Test
        fun `should not get Electoral Registration Office given API returns a 500 error`() {
            // Given
            given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(INTERNAL_SERVER_ERROR).build())
            )

            // When
            val ex = catchThrowable { apiClient.getElectoralRegistrationOfficeGssCodes(aValidRandomEroId()) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeGeneralException::class.java)
        }

        @Test
        fun `should throw exception given API returns a non WebClientResponseException`() {
            // Given
            val exception = object : WebClientException("general exception") {}
            given(exchangeFunction.exchange(any())).willReturn(Mono.error(exception))

            // When
            val ex = catchThrowable { apiClient.getElectoralRegistrationOfficeGssCodes(aValidRandomEroId()) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeGeneralException::class.java)
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
            given(clientResponse.statusCode()).willReturn(HttpStatus.OK)
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
                Mono.just(erosResponse)
            )

            val expected = buildEroDto()
            given(eroMapper.toEroDto(any(), any())).willReturn(expected)

            // When
            val ero = apiClient.getEro(gssCode)

            // Then
            assertThat(ero).isSameAs(expected)
            assertRequestUri(gssCode)
            verify(eroMapper).toEroDto(eroResponse.id, eroResponse.localAuthorities[0])
        }

        @Test
        fun `should throw exception given ero does not exist for the gssCode`() {
            // Given
            val gssCode = getRandomGssCode()
            val emptyResponse = ElectoralRegistrationOfficesResponse(emptyList())
            given(clientResponse.statusCode()).willReturn(HttpStatus.OK)
            given(clientResponse.bodyToMono(ElectoralRegistrationOfficesResponse::class.java)).willReturn(
                Mono.just(emptyResponse)
            )

            // When
            val ex = catchThrowable { apiClient.getEro(gssCode) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeNotFoundException::class.java)
            assertRequestUri(gssCode)
            verifyNoInteractions(eroMapper)
        }

        @Test
        fun `should throw exception given API returns a 404 error`() {
            // Given
            given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(NOT_FOUND).build())
            )

            // When
            val ex = catchThrowable { apiClient.getEro(getRandomGssCode()) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeNotFoundException::class.java)
            verifyNoInteractions(eroMapper)
        }

        @Test
        fun `should throw exception given API returns a 500 error`() {
            // Given
            given(exchangeFunction.exchange(any())).willReturn(
                Mono.just(ClientResponse.create(INTERNAL_SERVER_ERROR).build())
            )

            // When
            val ex = catchThrowable { apiClient.getEro(getRandomGssCode()) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeGeneralException::class.java)
            verifyNoInteractions(eroMapper)
        }

        @Test
        fun `should throw exception given API returns a non WebClientResponseException`() {
            // Given
            val exception = object : WebClientException("general exception") {}
            given(exchangeFunction.exchange(any())).willReturn(
                Mono.error(exception)
            )

            // When
            val ex = catchThrowable { apiClient.getEro(getRandomGssCode()) }

            // Then
            assertThat(ex).isNotNull().isInstanceOf(ElectoralRegistrationOfficeGeneralException::class.java)
            verifyNoInteractions(eroMapper)
        }
    }

    private fun assertRequestUri(gssCode: String) {
        assertThat(clientRequest.value.url()).hasHost("ero-management-api").hasPath("/eros")
            .hasQuery("gssCode=$gssCode")
    }
}
