package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto

fun EroContactDetailsDto.toElectoralRegistrationOffice(name: String) =
    ElectoralRegistrationOffice(
        name = name,
        phoneNumber = phoneNumber,
        website = website,
        emailAddress = emailAddress,
        address = with(address) {
            Address(
                property = property,
                street = street,
                town = town,
                area = area,
                postcode = postcode,
            )
        }
    )
