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
import net.datafaker.providers.base.Address as FakerAddress

fun buildLocalAuthorityResponse(
    gssCode: String = getRandomGssCode(),
    name: String = aValidLocalAuthorityName(),
    displayName: String = aValidLocalAuthorityName(),
    contactDetailsEnglish: ContactDetails = buildContactDetails(),
    contactDetailsWelsh: ContactDetails? = null,
) = LocalAuthorityResponse(
    gssCode = gssCode,
    name = name,
    displayName = displayName,
    contactDetailsEnglish = contactDetailsEnglish,
    contactDetailsWelsh = contactDetailsWelsh,
)

fun buildContactDetails(
    name: String = aValidLocalAuthorityName(),
    websiteAddress: String = aValidWebsite(),
    phoneNumber: String = aValidPhoneNumber(),
    emailAddress: String = aValidEmailAddress(),
    address: Address = buildEroManagementAddress(),
    nameVac: String = aValidLocalAuthorityName(),
    websiteVac: String = aValidWebsite(),
    emailAddressVac: String = aValidEmailAddress()
): ContactDetails =
    ContactDetails(
        name = name,
        nameVac = nameVac,
        website = websiteAddress,
        websiteVac = websiteVac,
        phone = phoneNumber,
        email = emailAddress,
        emailVac = emailAddressVac,
        address = address
    )

fun buildEroManagementAddress(
    fakeAddress: FakerAddress = faker.address(),
    street: String = fakeAddress.streetName(),
    postcode: String = fakeAddress.postcode(),
    property: String = fakeAddress.buildingNumber(),
    town: String = fakeAddress.city(),
    area: String = fakeAddress.state(),
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
