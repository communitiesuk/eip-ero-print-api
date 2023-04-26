package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.AedSearchSummary
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.testsupport.testdata.DataFaker.Companion.faker
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidElectoralRollNumber
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidGeneratedDateTime
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber
import java.time.LocalDate
import java.time.OffsetDateTime

fun buildAedSearchSummaryApi(
    gssCode: String = aGssCode(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    status: AnonymousElectorDocumentStatus = AnonymousElectorDocumentStatus.PRINTED,
    issueDate: LocalDate = aValidIssueDate(),
    dateTimeCreated: OffsetDateTime = aValidGeneratedDateTime(),
    firstName: String = aValidFirstName(),
    surname: String = aValidSurname(),
    postcode: String = faker.address().postcode(),
): AedSearchSummary {
    return AedSearchSummary(
        gssCode = gssCode,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        certificateNumber = certificateNumber,
        electoralRollNumber = electoralRollNumber,
        status = status,
        issueDate = issueDate,
        dateTimeCreated = dateTimeCreated,
        firstName = firstName,
        surname = surname,
        postcode = postcode
    )
}
