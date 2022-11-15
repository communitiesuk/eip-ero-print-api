package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto

@Mapper
abstract class EroManagementApiEroDtoMapper {

    @Mapping(target = "englishContactDetails", expression = "java( englishContactDetails() )")
    @Mapping(target = "welshContactDetails", expression = "java( welshContactDetails() )")
    abstract fun toEroManagementApiEroDto(eroResponse: ElectoralRegistrationOfficeResponse): EroManagementApiEroDto

    // TODO - to be replaced with mapping from ElectoralRegistrationOfficeResponse when ERO Management returns contact data
    protected fun englishContactDetails(): EroContactDetailsDto =
        EroContactDetailsDto(
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

    // TODO - to be replaced with mapping from ElectoralRegistrationOfficeResponse when ERO Management returns contact data
    protected fun welshContactDetails(): EroContactDetailsDto =
        EroContactDetailsDto(
            name = "Etholiadau Cyngor Gwynedd",
            phoneNumber = "01766 771000",
            website = "https://www.gwynedd.llyw.cymru/cy/Cyngor/Cysylltu-%c3%a2-ni/Cysylltu-%c3%a2-ni.aspx",
            emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
            address = AddressDto(
                property = "Pencadlys Cyngor Gwynedd",
                street = "Stryd y Jêl",
                town = "Caernarfon",
                area = "Gwynedd",
                postcode = "LL55 1SH",
            )
        )
}
