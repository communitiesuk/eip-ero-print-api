package uk.gov.dluhc.printapi.testsupport.testdata

import org.apache.commons.lang3.RandomStringUtils.randomAlphabetic
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.bson.types.ObjectId
import uk.gov.dluhc.printapi.testsupport.replaceSpacesWith
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit.SECONDS

fun aValidEroId() = "${faker.address().city().lowercase()}-city-council"
fun anotherValidEroId(refEroId: String): String {
    while (true) {
        val eroId = aValidEroId()

        if (eroId != refEroId) {
            return eroId
        }
    }
}

fun aValidEroName(): String = faker.address().city()
fun aValidLocalAuthorityName(): String = faker.address().city()

fun getRandomGssCode() = "E${randomNumeric(8)}"

fun getRandomGssCodeList() = listOf(getRandomGssCode())

/**
 * Returns a string that represents a mongodb id ([ObjectId](https://www.mongodb.com/docs/manual/reference/method/ObjectId/))
 * which are 24 character wide hex strings.
 */
fun getAMongoDbId(): String = ObjectId().toHexString()

fun aValidRequestId(): String = getAMongoDbId()

fun aValidRequestDateTime(): Instant = Instant.now().truncatedTo(SECONDS)

fun aValidVacNumber(): String = randomAlphanumeric(20)

fun aValidVacVersion(): String = randomAlphanumeric(20)

fun aValidSourceReference(): String = getAMongoDbId()

fun aValidApplicationReference(): String = "V${randomAlphabetic(9).uppercase()}"

fun aValidElectoralRollNumber(): String = "${randomAlphabetic(4)}-${randomNumeric(8)}" // no specific format. "Special" characters allowed

fun aValidApplicationReceivedDateTime(): Instant = Instant.now().truncatedTo(SECONDS)

fun aValidIssuingAuthority(): String = aValidLocalAuthorityName()

fun aValidIssueDate(): LocalDate = LocalDate.now()

fun aValidOnDate(): LocalDate = LocalDate.now()

fun aValidGeneratedDateTime(): OffsetDateTime = OffsetDateTime.now()

fun aValidSuggestedExpiryDate(): LocalDate = LocalDate.now().plusYears(10)

fun aValidFirstName(): String = faker.name().firstName()

fun aValidSurname(): String = faker.name().lastName()

fun aValidUserId(): String = faker.name().username()

fun aValidDeliveryName(): String = faker.name().fullName()

fun aValidAddressStreet(): String = faker.address().streetName()

fun aValidAddressPostcode(): String = faker.address().postcode()

fun aValidPhoneNumber(): String = faker.phoneNumber().cellPhone()

fun aValidEmailAddress(): String = "contact@${aValidEroName().replaceSpacesWith("-")}.gov.uk"

fun aValidWebsite(): String = "https://${aValidEroName().replaceSpacesWith("-")}.gov.uk"

fun aValidPrintRequestStatusEventDateTime(): Instant = Instant.now().truncatedTo(SECONDS)

fun aValidEventMessage(): String = faker.harryPotter().spell()
