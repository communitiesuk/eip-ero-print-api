package uk.gov.dluhc.printapi.rds.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.mapper.ElectoralRegistrationOfficeMapperImpl
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroManagementApiEroDto

class RdsElectoralRegistrationOfficeMapperTest {

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
        assertThat(electoralRegistrationOffice).usingRecursiveComparison().isEqualTo(expected)
    }
}
