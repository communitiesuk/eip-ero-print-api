package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.model.buildLocalAuthorityResponse

class EroDtoMapperTest {
    private val mapper = EroDtoMapperImpl()

    @Test
    fun `should map ERO response to Issuer dto`() {
        // Given
        val localAuthority = buildLocalAuthorityResponse(contactDetailsWelsh = null)
        val expected = with(localAuthority) {
            EroDto(
                englishContactDetails = with(localAuthority.contactDetailsEnglish) {
                    EroContactDetailsDto(
                        name = name,
                        emailAddress = email,
                        phoneNumber = phone,
                        website = website,
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
        }

        // When
        val actual = mapper.toIssuerDto(localAuthority)

        // Then
        Assertions.assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }

    @Test
    fun `should map ERO response with Welsh to Issuer dto`() {
        // Given
        val localAuthority = buildLocalAuthorityResponse(
            contactDetailsWelsh = buildContactDetails()
        )
        val expected = with(localAuthority) {
            EroDto(
                englishContactDetails = with(localAuthority.contactDetailsEnglish) {
                    EroContactDetailsDto(
                        name = name,
                        emailAddress = email,
                        phoneNumber = phone,
                        website = website,
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
                        name = name,
                        emailAddress = email,
                        phoneNumber = phone,
                        website = website,
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
        }

        // When
        val actual = mapper.toIssuerDto(localAuthority)

        // Then
        Assertions.assertThat(actual).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(expected)
    }
}
