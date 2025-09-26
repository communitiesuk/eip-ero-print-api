package uk.gov.dluhc.printapi.testsupport.testdata.messaging.model

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.printapi.messaging.models.Address
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import net.datafaker.providers.base.Address as FakerAddress

fun buildAddress(
    fakeAddress: FakerAddress = faker.address(),
    street: String = fakeAddress.streetName(),
    postcode: String = fakeAddress.postcode(),
    property: String? = fakeAddress.buildingNumber(),
    locality: String? = fakeAddress.streetName(),
    town: String? = fakeAddress.city(),
    area: String? = fakeAddress.state(),
    uprn: String? = RandomStringUtils.secure().nextNumeric(12)
) = Address(
    street,
    postcode,
    property,
    locality,
    town,
    area,
    uprn,
)
