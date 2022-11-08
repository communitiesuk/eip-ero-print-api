package uk.gov.dluhc.printapi.testsupport.testdata

import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric
import org.apache.commons.lang3.RandomStringUtils.randomNumeric
import org.bson.types.ObjectId
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.style.RFC4519Style.name
import uk.gov.dluhc.printapi.database.entity.CertificateFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.DeliveryMethod
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.replaceSpacesWith
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import java.time.LocalDate
import java.time.OffsetDateTime

fun aValidEroName(): String = faker.address().city()
fun aValidLocalAuthorityName(): String = faker.address().city()

fun getRandomGssCode() = "E${randomNumeric(8)}"

/**
 * Returns a string that represents a mongodb id ([ObjectId](https://www.mongodb.com/docs/manual/reference/method/ObjectId/))
 * which are 24 character wide hex strings.
 */
fun getAMongoDbId(): String = ObjectId().toHexString()

fun aValidRequestId(): String = getAMongoDbId()

fun aValidRequestDateTime(): OffsetDateTime = OffsetDateTime.now()

fun aValidVacNumber(): String = randomAlphanumeric(20)

fun aValidVacVersion(): String = randomAlphanumeric(20)

fun aValidSourceType() = SourceType.VOTER_CARD

fun aValidSourceReference(): String = getAMongoDbId()

fun aValidApplicationReference(): String = "V${RandomStringUtils.randomAlphabetic(9).uppercase()}"

fun aValidApplicationReceivedDateTime(): OffsetDateTime = OffsetDateTime.now()

fun aValidIssuingAuthority(): String = aValidLocalAuthorityName()

fun aValidIssueDate(): LocalDate = LocalDate.now()

fun aValidSuggestedExpiryDate(): LocalDate = LocalDate.now().plusYears(10)

fun aValidCertificateStatus() = Status.PENDING_ASSIGNMENT_TO_BATCH

fun aValidCertificateLanguage() = CertificateLanguage.EN

fun aValidCertificateFormat() = CertificateFormat.STANDARD

fun aValidFirstName(): String = faker.name().firstName()

fun aValidSurname(): String = faker.name().lastName()

fun aValidDeliveryName(): String = faker.name().fullName()

fun aValidDeliveryClass(): DeliveryClass = DeliveryClass.STANDARD

fun aValidDeliveryMethod(): DeliveryMethod = DeliveryMethod.DELIVERY

fun aValidAddressStreet(): String = faker.address().streetName()

fun aValidAddressPostcode(): String = faker.address().postcode()

fun aValidPhoneNumber(): String = faker.phoneNumber().cellPhone()

fun aValidEmailAddress(): String = "contact@${aValidEroName().replaceSpacesWith("-")}.gov.uk"

fun aValidWebsite(): String = "https://${aValidEroName().replaceSpacesWith("-")}.gov.uk"
