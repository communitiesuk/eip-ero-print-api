package uk.gov.dluhc.printapi.testsupport.testdata.entity

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun buildAddress(
    street: String = faker.address().streetName(),
    postcode: String = faker.address().postcode(),
    property: String? = faker.address().buildingNumber(),
    locality: String? = faker.address().streetName(),
    town: String? = faker.address().city(),
    area: String? = faker.address().state(),
    uprn: String? = RandomStringUtils.randomNumeric(12)
) = Address(
    street,
    postcode,
    property,
    locality,
    town,
    area,
    uprn,
)
