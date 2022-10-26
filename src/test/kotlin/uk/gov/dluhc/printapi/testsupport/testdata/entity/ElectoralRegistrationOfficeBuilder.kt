package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.replaceSpacesWith
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName

fun buildElectoralRegistrationOffice(
    name: String = aValidEroName(),
    phoneNumber: String = faker.phoneNumber().cellPhone(),
    emailAddress: String = "contact@${name.replaceSpacesWith("-")}.gov.uk",
    website: String = "https://${name.replaceSpacesWith("-")}.gov.uk",
    address: Address = buildAddress()
) =
    ElectoralRegistrationOffice(
        name = name,
        phoneNumber = phoneNumber,
        emailAddress = emailAddress,
        website = website,
        address = address
    )
