package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.CertificateDelivery
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.getAMongoDbId
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

fun buildPrintDetails(
    id: UUID = UUID.randomUUID(),
    requestId: String = getAMongoDbId(),
    sourceReference: String = getAMongoDbId(),
    applicationReference: String = aValidApplicationReference(),
    vacNumber: String = getAMongoDbId(),
    sourceType: SourceType = SourceType.VOTER_CARD,
    requestDateTime: OffsetDateTime = Instant.now().atOffset(UTC),
    firstName: String = faker.name().firstName(),
    middleNames: String? = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    delivery: CertificateDelivery = buildCertificateDelivery(),
    gssCode: String = getRandomGssCode(),
    issuingAuthority: String = aValidLocalAuthorityName(),
    issueDate: LocalDate = LocalDate.now(),
    eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthority),
    eroWelsh: ElectoralRegistrationOffice? = null,
    photoLocation: String = "arn:aws:s3:::source-document-storage/$gssCode/$sourceReference/${UUID.randomUUID()}/" +
        faker.file().fileName("", null, "jpg", ""),
    status: Status = Status.PENDING_ASSIGNMENT_TO_BATCH,
    batchId: String? = UUID.randomUUID().toString()
) = PrintDetails(
    id = id,
    requestId = requestId,
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    sourceType = sourceType,
    vacNumber = vacNumber,
    requestDateTime = requestDateTime,
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    certificateLanguage = certificateLanguage,
    photoLocation = photoLocation,
    delivery = delivery,
    gssCode = gssCode,
    issuingAuthority = issuingAuthority,
    issueDate = issueDate,
    eroEnglish = eroEnglish,
    eroWelsh = eroWelsh,
    batchId = batchId,
    status = status,
)

fun aPrintDetailsList() = listOf(buildPrintDetails())
