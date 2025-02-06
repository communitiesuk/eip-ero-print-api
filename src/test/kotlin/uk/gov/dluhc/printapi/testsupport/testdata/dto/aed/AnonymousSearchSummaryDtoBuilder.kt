package uk.gov.dluhc.printapi.testsupport.testdata.dto.aed

import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidRequestDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import java.time.Instant
import java.time.LocalDate

fun buildAnonymousSearchSummaryDto(
    gssCode: String = aGssCode(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    issueDate: LocalDate = aValidIssueDate(),
    dateTimeCreated: Instant = aValidRequestDateTime(),
    firstName: String = aValidFirstName(),
    surname: String = aValidSurname(),
    postcode: String? = faker.address().postcode(),
): AnonymousSearchSummaryDto {
    return AnonymousSearchSummaryDto(
        gssCode = gssCode,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        certificateNumber = certificateNumber,
        electoralRollNumber = electoralRollNumber,
        issueDate = issueDate,
        dateTimeCreated = dateTimeCreated,
        firstName = firstName,
        surname = surname,
        postcode = postcode
    )
}
