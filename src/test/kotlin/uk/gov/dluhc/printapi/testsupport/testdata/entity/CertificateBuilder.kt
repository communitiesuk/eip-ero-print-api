package uk.gov.dluhc.printapi.testsupport.testdata.entity

import org.apache.commons.lang3.RandomStringUtils
import uk.gov.dluhc.printapi.database.entity.Address
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.database.entity.Delivery
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.database.entity.PrintRequest
import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReceivedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateFormat
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryClass
import uk.gov.dluhc.printapi.testsupport.testdata.aValidDeliveryMethod
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
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacVersion
import uk.gov.dluhc.printapi.testsupport.testdata.aValidWebsite
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.Instant
import java.util.UUID

fun certificateBuilder(
    id: UUID? = UUID.randomUUID(),
    status: Status = aValidCertificateStatus(),
    printRequests: List<PrintRequest> = listOf(printRequestBuilder()),
): Certificate {
    val certificate = Certificate(
        id = id,
        vacNumber = aValidVacNumber(),
        sourceType = aValidSourceType(),
        sourceReference = aValidSourceReference(),
        applicationReference = aValidApplicationReference(),
        applicationReceivedDateTime = aValidApplicationReceivedDateTime(),
        issuingAuthority = aValidIssuingAuthority(),
        issueDate = aValidIssueDate(),
        suggestedExpiryDate = aValidSuggestedExpiryDate(),
        gssCode = aGssCode(),
        status = status
    )
    printRequests.forEach { printRequest -> certificate.addPrintRequest(printRequest) }
    return certificate
}

fun printRequestBuilder(
    requestId: String = aValidRequestId(),
    printRequestStatuses: List<PrintRequestStatus> = listOf(printRequestStatusBuilder()),
    requestDateTime: Instant? = aValidRequestDateTime(),
    eroEnglish: ElectoralRegistrationOffice = electoralRegistrationOfficeBuilder(),
    eroWelsh: ElectoralRegistrationOffice? = null,
    delivery: Delivery = deliveryBuilder(),
    batchId: String? = null,
    photoLocationArn: String? = aPhotoArn(),
): PrintRequest {
    val printRequest = PrintRequest(
        requestId = requestId,
        vacVersion = aValidVacVersion(),
        requestDateTime = requestDateTime,
        firstName = aValidFirstName(),
        surname = aValidSurname(),
        certificateLanguage = aValidCertificateLanguage(),
        certificateFormat = aValidCertificateFormat(),
        photoLocationArn = photoLocationArn,
        delivery = delivery,
        eroEnglish = eroEnglish,
        eroWelsh = eroWelsh,
        userId = aValidUserId(),
        batchId = batchId
    )
    printRequestStatuses.forEach { printRequestStatus -> printRequest.addPrintRequestStatus(printRequestStatus) }
    return printRequest
}

fun printRequestStatusBuilder(
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

fun electoralRegistrationOfficeBuilder(
    name: String = aValidEroName(),
    address: Address = addressBuilder()
): ElectoralRegistrationOffice {
    return ElectoralRegistrationOffice(
        address = address,
        name = name,
        phoneNumber = aValidPhoneNumber(),
        emailAddress = aValidEmailAddress(),
        website = aValidWebsite()
    )
}

fun addressBuilder(
    street: String = DataFaker.faker.address().streetName(),
    postcode: String = DataFaker.faker.address().postcode(),
    property: String? = DataFaker.faker.address().buildingNumber(),
    locality: String? = DataFaker.faker.address().streetName(),
    town: String? = DataFaker.faker.address().city(),
    area: String? = DataFaker.faker.address().state(),
    uprn: String? = RandomStringUtils.randomNumeric(12)
) = Address(
    street = street,
    postcode = postcode,
    property = property,
    locality = locality,
    town = town,
    area = area,
    uprn = uprn,
)

fun deliveryBuilder(): Delivery {
    return Delivery(
        addressee = aValidDeliveryName(),
        address = addressBuilder(),
        deliveryClass = aValidDeliveryClass(),
        deliveryMethod = aValidDeliveryMethod()
    )
}
