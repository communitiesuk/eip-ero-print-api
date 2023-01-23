package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.messaging.models.CertificateDelivery
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.messaging.models.SourceType
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit
import java.util.UUID

fun buildSendApplicationToPrintMessage(
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    requestDateTime: OffsetDateTime = Instant.now().atOffset(UTC).truncatedTo(ChronoUnit.SECONDS),
    applicationReceivedDateTime: OffsetDateTime = Instant.now().atOffset(UTC),
    firstName: String = faker.name().firstName(),
    middleNames: String? = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    supportingInformationFormat: SupportingInformationFormat = SupportingInformationFormat.STANDARD,
    delivery: CertificateDelivery = buildCertificateDelivery(),
    gssCode: String = getRandomGssCode(),
    photoLocation: String = "arn:aws:s3:::source-document-storage/$gssCode/$sourceReference/${UUID.randomUUID()}/" +
        faker.file().fileName("", null, "jpg", ""),
    userId: String = faker.internet().emailAddress()
) = SendApplicationToPrintMessage(
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    sourceType = sourceType,
    requestDateTime = requestDateTime,
    applicationReceivedDateTime = applicationReceivedDateTime,
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    certificateLanguage = certificateLanguage,
    supportingInformationFormat = supportingInformationFormat,
    photoLocation = photoLocation,
    delivery = delivery,
    gssCode = gssCode,
    userId = userId
)
