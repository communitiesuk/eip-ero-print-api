package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRandomEroId
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse

class EroDtoMapperTest {
    private val mapper = EroDtoMapperImpl()

    @Test
    fun `should map ERO response to ERO dto`() {
        // Given
        val eroId = aValidRandomEroId()
        val localAuthority = buildLocalAuthorityResponse(contactDetailsWelsh = null)
        val expected = EroDto(
            eroId = eroId,
            englishContactDetails = with(localAuthority.contactDetailsEnglish) {
                EroContactDetailsDto(
                    name = nameVac,
                    emailAddress = emailVac!!,
                    phoneNumber = phone,
                    website = websiteVac,
                    address = with(address) {
                        AddressDto(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    }
                )
            },
            welshContactDetails = null
        )

        // When
        val actual = mapper.toEroDto(eroId, localAuthority)

        // Then
        Assertions.assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }

    @Test
    fun `should map ERO response with Welsh to ERO dto`() {
        // Given
        val eroId = aValidRandomEroId()
        val localAuthority = buildLocalAuthorityResponse(
            contactDetailsWelsh = buildContactDetails()
        )
        val expected = EroDto(
            eroId = eroId,
            englishContactDetails = with(localAuthority.contactDetailsEnglish) {
                EroContactDetailsDto(
                    name = nameVac,
                    emailAddress = emailVac!!,
                    phoneNumber = phone,
                    website = websiteVac,
                    address = with(address) {
                        AddressDto(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    }
                )
            },
            welshContactDetails = with(localAuthority.contactDetailsWelsh!!) {
                EroContactDetailsDto(
                    name = nameVac,
                    emailAddress = emailVac!!,
                    phoneNumber = phone,
                    website = websiteVac,
                    address = with(address) {
                        AddressDto(
                            street = street,
                            postcode = postcode,
                            property = property,
                            locality = locality,
                            town = town,
                            area = area,
                            uprn = uprn
                        )
                    }
                )
            }
        )

        // When
        val actual = mapper.toEroDto(eroId, localAuthority)

        // Then
        Assertions.assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }
}
