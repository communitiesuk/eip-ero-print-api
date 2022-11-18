package uk.gov.dluhc.printapi.service

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeGeneralException
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId

@ExtendWith(MockitoExtension::class)
internal class EroServiceTest {

    @Mock
    private lateinit var electoralRegistrationOfficeManagementApiClient: ElectoralRegistrationOfficeManagementApiClient

    @InjectMocks
    private lateinit var eroService: EroService

    @Test
    fun `should lookup and return gssCodes`() {
        // Given
        val eroId = aValidRandomEroId()

        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any())).willReturn(
            ElectoralRegistrationOfficeResponse(
                id = eroId,
                name = "Test ERO",
                localAuthorities = listOf(
                    LocalAuthorityResponse(
                        gssCode = "E123456789",
                        name = "Local Authority 1"
                    ),
                    LocalAuthorityResponse(
                        gssCode = "E987654321",
                        name = "Local Authority 2"
                    ),
                )
            )
        )

        val expectedGssCodes = listOf("E123456789", "E987654321")

        // When
        val gssCodes = eroService.lookupGssCodesForEro(eroId)

        // Then
        assertThat(gssCodes).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expectedGssCodes)
        verify(electoralRegistrationOfficeManagementApiClient).getElectoralRegistrationOffice(eroId)
    }

    @Test
    fun `should not return gssCodes given API client throws ERO not found exception`() {
        // Given
        val eroId = aValidRandomEroId()

        val expected = ElectoralRegistrationOfficeNotFoundException(eroId)
        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any())).willThrow(expected)

        // When
        val ex = Assertions.catchThrowableOfType(
            { eroService.lookupGssCodesForEro(eroId) },
            ElectoralRegistrationOfficeNotFoundException::class.java
        )

        // Then
        assertThat(ex).isEqualTo(expected)
    }

    @Test
    fun `should not return gssCodes given API client throws general exception`() {
        // Given
        val eroId = aValidRandomEroId()

        val expected = ElectoralRegistrationOfficeGeneralException("Some error getting ERO $eroId")
        given(electoralRegistrationOfficeManagementApiClient.getElectoralRegistrationOffice(any())).willThrow(expected)

        // When
        val ex = Assertions.catchThrowableOfType(
            { eroService.lookupGssCodesForEro(eroId) },
            ElectoralRegistrationOfficeGeneralException::class.java
        )

        // Then
        assertThat(ex).isEqualTo(expected)
    }
}
