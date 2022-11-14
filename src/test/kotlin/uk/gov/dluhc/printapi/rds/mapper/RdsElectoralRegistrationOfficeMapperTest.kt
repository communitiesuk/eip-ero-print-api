package uk.gov.dluhc.printapi.rds.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.mapper.ElectoralRegistrationOfficeMapperImpl
import uk.gov.dluhc.printapi.rds.entity.Address
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice

class RdsElectoralRegistrationOfficeMapperTest {

    private val mapper = ElectoralRegistrationOfficeMapperImpl()

    @Test
    fun `should map`() {
        // Given
        val dto = EroContactDetailsDto(
            name = "Gwynedd Council Elections",
            phoneNumber = "01766 771000",
            website = "https://www.gwynedd.llyw.cymru/en/Council/Contact-us/Contact-us.aspx",
            emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
            address = AddressDto(
                property = "Gwynedd Council Headquarters",
                street = "Shirehall Street",
                town = "Caernarfon",
                area = "Gwynedd",
                postcode = "LL55 1SH",
            )
        )

        val expected = ElectoralRegistrationOffice(
            name = "Gwynedd Council Elections",
            phoneNumber = "01766 771000",
            website = "https://www.gwynedd.llyw.cymru/en/Council/Contact-us/Contact-us.aspx",
            emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
            address = Address(
                property = "Gwynedd Council Headquarters",
                street = "Shirehall Street",
                town = "Caernarfon",
                area = "Gwynedd",
                postcode = "LL55 1SH",
            )
        )

        // When
        val electoralRegistrationOffice = mapper.map(dto)

        // Then
        assertThat(electoralRegistrationOffice).usingRecursiveComparison().isEqualTo(expected)
    }
}
