package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.CertificateDelivery
import uk.gov.dluhc.printapi.database.entity.CertificateFormat
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintDetails
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidLocalAuthorityName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.getRandomGssCode
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID

fun buildPrintDetails(
    id: UUID = UUID.randomUUID(),
    requestId: String = aValidRequestId(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    vacNumber: String = aValidVacNumber(),
    vacVersion: String = "1",
    sourceType: SourceType = SourceType.VOTER_CARD,
    requestDateTime: OffsetDateTime = Instant.now().atOffset(UTC),
    firstName: String = faker.name().firstName(),
    middleNames: String? = faker.name().firstName(),
    surname: String = faker.name().lastName(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    certificateFormat: CertificateFormat = CertificateFormat.STANDARD,
    delivery: CertificateDelivery = buildCertificateDelivery(),
    gssCode: String = getRandomGssCode(),
    issuingAuthority: String = aValidLocalAuthorityName(),
    issueDate: LocalDate = LocalDate.now(),
    suggestedExpiryDate: LocalDate = issueDate.plusYears(10),
    eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(name = issuingAuthority),
    eroWelsh: ElectoralRegistrationOffice? = null,
    photoLocation: String = "arn:aws:s3:::source-document-storage/$gssCode/$sourceReference/${UUID.randomUUID()}/" +
        faker.file().fileName("", null, "jpg", ""),
    printRequestStatuses: MutableList<PrintRequestStatus> = mutableListOf(
        PrintRequestStatus(Status.PENDING_ASSIGNMENT_TO_BATCH, OffsetDateTime.now(UTC))
    ),
    batchId: String? = aValidBatchId(),
) = PrintDetails(
    id = id,
    requestId = requestId,
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    sourceType = sourceType,
    vacNumber = vacNumber,
    vacVersion = vacVersion,
    requestDateTime = requestDateTime,
    firstName = firstName,
    middleNames = middleNames,
    surname = surname,
    certificateLanguage = certificateLanguage,
    certificateFormat = certificateFormat,
    photoLocation = photoLocation,
    delivery = delivery,
    gssCode = gssCode,
    issuingAuthority = issuingAuthority,
    issueDate = issueDate,
    suggestedExpiryDate = suggestedExpiryDate,
    eroEnglish = eroEnglish,
    eroWelsh = eroWelsh,
    batchId = batchId,
    printRequestStatuses = printRequestStatuses,
)

fun buildPrintDetails(
    status: Status = Status.PENDING_ASSIGNMENT_TO_BATCH,
    batchId: String? = aValidBatchId(),
) = buildPrintDetails(
    batchId = batchId,
    printRequestStatuses = mutableListOf(
        PrintRequestStatus(status, OffsetDateTime.now(UTC))
    )
)

fun aPrintDetailsList() = listOf(buildPrintDetails())
