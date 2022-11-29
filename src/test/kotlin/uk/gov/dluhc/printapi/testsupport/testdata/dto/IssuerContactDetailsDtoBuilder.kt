package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.dto.IssuerContactDetailsDto

fun anEnglishIssuerContactDetails(): IssuerContactDetailsDto =
    IssuerContactDetailsDto(
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

fun aWelshIssuerContactDetails(): IssuerContactDetailsDto =
    IssuerContactDetailsDto(
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
