package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.Address
import uk.gov.dluhc.printapi.messaging.models.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.testsupport.replaceSpacesWith
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomEro

fun buildElectoralRegistrationOffice(
    name: String = getRandomEro(),
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
