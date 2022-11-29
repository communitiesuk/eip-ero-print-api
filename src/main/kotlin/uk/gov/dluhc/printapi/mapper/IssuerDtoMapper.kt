package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.eromanagementapi.models.ContactDetails
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.dto.IssuerContactDetailsDto
import uk.gov.dluhc.printapi.dto.IssuerDto

@Mapper
abstract class IssuerDtoMapper {

    @Mapping(target = "englishContactDetails", expression = "java(toIssuerContactDetailsDto( localAuthority.getName(), localAuthority.getContactDetailsEnglish() ))")
    @Mapping(target = "welshContactDetails", expression = "java(toNullIssuerContactDetailsDto( localAuthority.getName(), localAuthority.getContactDetailsWelsh() ))")
    abstract fun toIssuerDto(localAuthority: LocalAuthorityResponse): IssuerDto

    fun toNullIssuerContactDetailsDto(name: String, contactDetails: ContactDetails?): IssuerContactDetailsDto? {
        return if (contactDetails == null) {
            null
        } else {
            toIssuerContactDetailsDto(name, contactDetails)
        }
    }

    @Mapping(target = "emailAddress", source = "contactDetails.email")
    @Mapping(target = "phoneNumber", source = "contactDetails.phone")
    abstract fun toIssuerContactDetailsDto(name: String, contactDetails: ContactDetails): IssuerContactDetailsDto
}
