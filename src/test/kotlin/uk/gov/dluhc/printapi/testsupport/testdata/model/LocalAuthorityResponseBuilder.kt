package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.eromanagementapi.models.Address
import uk.gov.dluhc.eromanagementapi.models.ContactDetails
import uk.gov.dluhc.eromanagementapi.models.LocalAuthorityResponse
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidWebsite
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode

fun buildLocalAuthorityResponse(
    gssCode: String = getRandomGssCode(),
    name: String = aValidLocalAuthorityName(),
    contactDetailsEnglish: ContactDetails = buildContactDetails(),
    contactDetailsWelsh: ContactDetails? = null,
) = LocalAuthorityResponse(
    gssCode = gssCode,
    name = name,
    contactDetailsEnglish = contactDetailsEnglish,
    contactDetailsWelsh = contactDetailsWelsh,
)

fun buildContactDetails(
    name: String = aValidLocalAuthorityName(),
    websiteAddress: String = aValidWebsite(),
    phoneNumber: String = aValidPhoneNumber(),
    emailAddress: String = aValidEmailAddress(),
    address: Address = buildEroManagementAddress(),
): ContactDetails =
    ContactDetails(
        name = name,
        website = websiteAddress,
        phone = phoneNumber,
        email = emailAddress,
        address = address
    )

fun buildEroManagementAddress(
    street: String = faker.address().streetName(),
    postcode: String = faker.address().postcode(),
    property: String = faker.address().buildingNumber(),
    town: String = faker.address().city(),
    area: String = faker.address().state(),
    locality: String? = null,
    uprn: String? = null,
): Address =
    Address(
        street = street,
        postcode = postcode,
        property = property,
        area = area,
        town = town,
        locality = locality,
        uprn = uprn
    )
