package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.CertificateSummaryDto
import uk.gov.dluhc.printapi.dto.DeliveryAddressType
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.dto.PrintRequestSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateStatus
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestStatusEventDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import java.time.Instant

fun buildCertificateSummaryDto(
    vacNumber: String = aValidVacNumber(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    firstName: String = aValidFirstName(),
    surname: String = aValidSurname(),
    middleNames: String? = null,
    printRequests: List<PrintRequestSummaryDto> = mutableListOf(buildPrintRequestSummaryDto())
) = CertificateSummaryDto(
    vacNumber = vacNumber,
    sourceReference = sourceReference,
    applicationReference = applicationReference,
    firstName = firstName,
    surname = surname,
    middleNames = middleNames,
    printRequests = printRequests
)

fun buildPrintRequestSummaryDto(
    userId: String = aValidUserId(),
    status: PrintRequestStatusDto = PrintRequestStatusDto.valueOf(aValidCertificateStatus().name),
    eventDateTime: Instant = aValidPrintRequestStatusEventDateTime(),
    message: String? = null,
    deliveryAddressType: DeliveryAddressType? = DeliveryAddressType.REGISTERED
) = PrintRequestSummaryDto(
    userId = userId,
    status = status,
    dateTime = eventDateTime,
    message = message,
    deliveryAddressType = deliveryAddressType
)
