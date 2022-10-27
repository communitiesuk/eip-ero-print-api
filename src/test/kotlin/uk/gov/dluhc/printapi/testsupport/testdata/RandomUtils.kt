package uk.gov.dluhc.printapi.testsupport.testdata

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.bson.types.ObjectId
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker

fun aValidEroName(): String = faker.address().city()
fun aValidLocalAuthorityName(): String = faker.address().city()

fun getRandomGssCode() = "E${randomNumeric(8)}"

/**
 * Returns a string that represents a mongodb id ([ObjectId](https://www.mongodb.com/docs/manual/reference/method/ObjectId/))
 * which are 24 character wide hex strings.
 */
fun getAMongoDbId(): String = ObjectId().toHexString()

fun aValidRequestId(): String = getAMongoDbId()

fun aValidVacNumber(): String = randomAlphanumeric(20)

fun aValidSourceReference(): String = getAMongoDbId()

fun aValidApplicationReference(): String = "V${RandomStringUtils.randomAlphabetic(9).uppercase()}"
