package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto
import uk.gov.dluhc.printapi.dto.VacPrintRequestSummaryDto
import uk.gov.dluhc.printapi.dto.VacSearchSummaryResults
import uk.gov.dluhc.printapi.dto.VacSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidPrintRequestStatusEventDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import java.time.Instant

fun buildVacSearchSummaryResults(
    page: Int = 1,
    pageSize: Int = 100,
    totalPages: Int = 1,
    results: List<VacSummaryDto> = listOf(buildVacSummaryDto()),
    totalResults: Int = results.size,
): VacSearchSummaryResults {
    return VacSearchSummaryResults(
        page = page,
        pageSize = pageSize,
        totalPages = totalPages,
        totalResults = totalResults,
        results = results,
    )
}

fun buildVacSummaryDto(
    vacNumber: String = aValidVacNumber(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    firstName: String = aValidFirstName(),
    surname: String = aValidSurname(),
    middleNames: String? = null,
    printRequests: List<VacPrintRequestSummaryDto> = listOf(buildVacPrintRequestSummaryDto())
): VacSummaryDto {
    return VacSummaryDto(
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        firstName = firstName,
        middleNames = middleNames,
        surname = surname,
        vacNumber = vacNumber,
        printRequests = printRequests
    )
}

fun buildVacPrintRequestSummaryDto(
    userId: String = aValidUserId(),
    status: PrintRequestStatusDto = PrintRequestStatusDto.DISPATCHED,
    dateTime: Instant = aValidPrintRequestStatusEventDateTime(),
): VacPrintRequestSummaryDto {
    return VacPrintRequestSummaryDto(
        dateTime = dateTime,
        status = status,
        userId = userId
    )
}
