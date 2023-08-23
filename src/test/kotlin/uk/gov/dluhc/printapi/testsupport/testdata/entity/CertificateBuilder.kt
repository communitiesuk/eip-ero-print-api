package uk.gov.dluhc.printapi.testsupport.testdata.entity

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.AddressFormat
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryClass
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus.Status
import uk.gov.dluhc.printapi.database.entity.SourceType
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidAddressFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidBatchId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryAddressType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEmailAddress
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssuingAuthority
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPhoneNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestStatusEventDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSuggestedExpiryDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSupportingInformationFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacVersion
import uk.gov.dluhc.printapi.testsupport.testdata.aValidWebsite
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID
import net.datafaker.providers.base.Address as DataFakerAddress

fun buildCertificate(
    id: UUID? = randomUUID(),
    vacNumber: String = aValidVacNumber(),
    status: Status = aValidCertificateStatus(),
    batchId: String = aValidBatchId(),
    printRequests: List<PrintRequest> = listOf(
        buildPrintRequest(
            batchId = batchId,
            printRequestStatuses = listOf(buildPrintRequestStatus(status = status))
        )
    ),
    gssCode: String = aGssCode(),
    photoLocationArn: String = aPhotoArn(),
    sourceType: SourceType = aValidSourceType(),
    sourceReference: String = aValidSourceReference(),
    applicationReceivedDateTime: Instant = aValidApplicationReceivedDateTime(),
    applicationReference: String = aValidApplicationReference(),
    issueDate: LocalDate = aValidIssueDate(),
    initialRetentionRemovalDate: LocalDate? = null,
    initialRetentionDataRemoved: Boolean = false,
    finalRetentionRemovalDate: LocalDate? = null,
): Certificate {
    val certificate = Certificate(
        id = id,
        vacNumber = vacNumber,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        applicationReceivedDateTime = applicationReceivedDateTime,
        issuingAuthority = aValidIssuingAuthority(),
        issueDate = issueDate,
        suggestedExpiryDate = aValidSuggestedExpiryDate(),
        gssCode = gssCode,
        photoLocationArn = photoLocationArn,
        status = status,
        initialRetentionRemovalDate = initialRetentionRemovalDate,
        initialRetentionDataRemoved = initialRetentionDataRemoved,
        finalRetentionRemovalDate = finalRetentionRemovalDate
    )
    printRequests.forEach { printRequest -> certificate.addPrintRequest(printRequest) }
    return certificate
}

fun buildPrintRequest(
    requestId: String = aValidRequestId(),
    printRequestStatuses: List<PrintRequestStatus> = listOf(buildPrintRequestStatus()),
    requestDateTime: Instant? = aValidRequestDateTime(),
    eroEnglish: ElectoralRegistrationOffice = buildElectoralRegistrationOffice(),
    eroWelsh: ElectoralRegistrationOffice? = null,
    delivery: Delivery? = buildDelivery(),
    batchId: String? = null,
    userId: String = aValidUserId(),
    supportingInformationFormat: SupportingInformationFormat? = aValidSupportingInformationFormat(),
): PrintRequest {
    val printRequest = PrintRequest(
        requestId = requestId,
        vacVersion = aValidVacVersion(),
        requestDateTime = requestDateTime,
        firstName = aValidFirstName(),
        surname = aValidSurname(),
        certificateLanguage = aValidCertificateLanguage(),
        supportingInformationFormat = supportingInformationFormat,
        delivery = delivery,
        eroEnglish = eroEnglish,
        eroWelsh = eroWelsh,
        userId = userId,
        batchId = batchId
    )
    printRequestStatuses.forEach { printRequestStatus -> printRequest.addPrintRequestStatus(printRequestStatus) }
    return printRequest
}

fun buildPrintRequestStatus(
    status: Status = aValidCertificateStatus(),
    eventDateTime: Instant = aValidPrintRequestStatusEventDateTime(),
    message: String? = null
): PrintRequestStatus {
    return PrintRequestStatus(
        status = status,
        eventDateTime = eventDateTime,
        message = message
    )
}

fun buildElectoralRegistrationOffice(
    name: String = aValidEroName(),
    address: Address = buildAddress()
): ElectoralRegistrationOffice {
    return ElectoralRegistrationOffice(
        address = address,
        name = name,
        phoneNumber = aValidPhoneNumber(),
        emailAddress = aValidEmailAddress(),
        website = aValidWebsite()
    )
}

fun buildAddress(
    id: UUID? = randomUUID(),
    fakeAddress: DataFakerAddress = faker.address(),
    street: String = fakeAddress.streetName(),
    postcode: String = fakeAddress.postcode(),
    property: String? = fakeAddress.buildingNumber(),
    locality: String? = fakeAddress.streetName(),
    town: String? = fakeAddress.city(),
    area: String? = fakeAddress.state(),
    uprn: String? = RandomStringUtils.randomNumeric(12)
) = Address(
    id = id,
    street = street,
    postcode = postcode,
    property = property,
    locality = locality,
    town = town,
    area = area,
    uprn = uprn,
)

fun buildDelivery(
    id: UUID? = randomUUID(),
    addressee: String = aValidDeliveryName(),
    address: Address = buildAddress(),
    deliveryClass: DeliveryClass = aValidDeliveryClass(),
    deliveryAddressType: DeliveryAddressType = aValidDeliveryAddressType(),
    collectionReason: String? = null,
    addressFormat: AddressFormat = aValidAddressFormat(),
): Delivery = Delivery(
    id = id,
    addressee = addressee,
    address = address,
    deliveryClass = deliveryClass,
    deliveryAddressType = deliveryAddressType,
    collectionReason = collectionReason,
    addressFormat = addressFormat,
)
