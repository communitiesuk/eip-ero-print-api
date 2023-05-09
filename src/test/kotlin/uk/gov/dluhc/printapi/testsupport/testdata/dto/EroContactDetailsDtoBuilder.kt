package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto

fun anEnglishEroContactDetails(
    address: AddressDto = anEnglishEroAddress(),
    emailAddress: String = "TrethCyngor@gwynedd.llyw.cymru",
): EroContactDetailsDto =
    EroContactDetailsDto(
        name = "Gwynedd Council Elections",
        phoneNumber = "01766 771000",
        website = "https://www.gwynedd.llyw.cymru/en/Council/Contact-us/Contact-us.aspx",
        emailAddress = emailAddress,
        address = address
    )

fun anEnglishEroAddress(): AddressDto = AddressDto(
    property = "Gwynedd Council Headquarters",
    street = "Shirehall Street",
    town = "Caernarfon",
    area = "Gwynedd",
    postcode = "LL55 1SH",
)

fun aWelshEroContactDetails(
    address: AddressDto = aWelshEroAddress(),
): EroContactDetailsDto =
    EroContactDetailsDto(
        name = "Etholiadau Cyngor Gwynedd",
        phoneNumber = "01766 771000",
        website = "https://www.gwynedd.llyw.cymru/cy/Cyngor/Cysylltu-%c3%a2-ni/Cysylltu-%c3%a2-ni.aspx",
        emailAddress = "TrethCyngor@gwynedd.llyw.cymru",
        address = address
    )

fun aWelshEroAddress(): AddressDto = AddressDto(
    property = "Pencadlys Cyngor Gwynedd",
    street = "Stryd y Jêl",
    town = "Caernarfon",
    area = "Gwynedd",
    postcode = "LL55 1SH",
)
