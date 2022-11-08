package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto

class ElectoralRegistrationOfficeMapperTest {

    private val mapper = ElectoralRegistrationOfficeMapperImpl()

    @Test
    fun `should map`() {
        // Given
        val dto = buildEroManagementApiEroDto(
            id = "croydon-london-borough-council",
            name = "Croydon London Borough Council"
        )

        val expected = ElectoralRegistrationOffice(
            name = "Croydon London Borough Council",
            phoneNumber = "",
            emailAddress = "",
            website = "",
            address = Address(
                street = "",
                postcode = ""
            )
        )

        // When
        val electoralRegistrationOffice = mapper.map(dto)

        // Then
        assertThat(electoralRegistrationOffice).isEqualTo(expected)
    }
}
