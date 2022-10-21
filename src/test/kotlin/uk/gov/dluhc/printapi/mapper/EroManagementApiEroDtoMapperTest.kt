package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.dto.EroManagementApiLocalAuthorityDto
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildElectoralRegistrationOfficeResponse

class EroManagementApiEroDtoMapperTest {
    private val mapper = EroManagementApiEroDtoMapperImpl()

    @Test
    fun `should map ERO response to dto`() {
        // Given
        val eroResponse = buildElectoralRegistrationOfficeResponse()
        val expected = with(eroResponse) {
            EroManagementApiEroDto(
                id = id,
                name = name,
                localAuthorities = localAuthorities.map {
                    EroManagementApiLocalAuthorityDto(
                        gssCode = it.gssCode!!,
                        name = it.name!!
                    )
                }
            )
        }

        // When
        val actual = mapper.toEroManagementApiEroDto(eroResponse)

        // Then
        assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }
}
