package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.CertificateDelivery
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.messaging.models.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.getAMongoDbId
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomEro
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

fun buildSendApplicationToPrintMessage(
    sourceReference: String = getAMongoDbId(),
    applicationReference: String = aValidApplicationReference(),
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    issuingAuthority: String = getRandomEro(),
    requestDateTime: OffsetDateTime = Instant.now().atOffset(UTC),
    firstName: String = faker.name().firstName(),
    middleNames: String = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    delivery: CertificateDelivery = buildCertificateDelivery(),
    eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthority),
    gssCode: String = getRandomGssCode(),
    photoLocation: String = "arn:aws:s3:::source-document-storage/$gssCode/$sourceReference/${UUID.randomUUID()}/${faker.file().fileName("", null, "jpg", "")}",
    eroWelsh: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthority)
) = SendApplicationToPrintMessage(
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    sourceType = sourceType,
    issuingAuthority = issuingAuthority,
    requestDateTime = requestDateTime,
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    certificateLanguage = certificateLanguage,
    photoLocation = photoLocation,
    delivery = delivery,
    eroEnglish = eroEnglish,
    gssCode = gssCode,
    eroWelsh = eroWelsh
)
