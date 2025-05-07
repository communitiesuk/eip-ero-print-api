package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.eromanagementapi.models.ContactDetails
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto

@Mapper
abstract class EroDtoMapper {

    @Mapping(target = "englishContactDetails", expression = "java(toEroContactDetailsDto( localAuthority.getContactDetailsEnglish() ))")
    @Mapping(target = "welshContactDetails", expression = "java(toNullEroContactDetailsDto( localAuthority.getContactDetailsWelsh() ))")
    abstract fun toEroDto(eroId: String, localAuthority: LocalAuthorityResponse): EroDto

    fun toNullEroContactDetailsDto(contactDetails: ContactDetails?): EroContactDetailsDto? {
        return if (contactDetails == null) {
            null
        } else {
            toEroContactDetailsDto(contactDetails)
        }
    }

    @Mapping(target = "name", source = "contactDetails.nameVac")
    @Mapping(target = "website", source = "contactDetails.websiteVac")
    @Mapping(target = "emailAddress", expression = "java((contactDetails.getEmailVac() != null) ? contactDetails.getEmailVac() : contactDetails.getEmail())")
    @Mapping(target = "phoneNumber", source = "contactDetails.phone")
    abstract fun toEroContactDetailsDto(contactDetails: ContactDetails): EroContactDetailsDto
}
