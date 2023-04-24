package uk.gov.dluhc.printapi.testsupport.testdata.entity

import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary
import uk.gov.dluhc.printapi.database.entity.SourceType
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
import uk.gov.dluhc.printapi.testsupport.testdata.anAnonymousElectorDocumentSourceType
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun buildAnonymousElectorDocumentSummaryEntity(
    id: UUID = UUID.randomUUID(),
    gssCode: String = aGssCode(),
    sourceType: SourceType = anAnonymousElectorDocumentSourceType(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    certificateNumber: String = aValidVacNumber(),
    electoralRollNumber: String = aValidElectoralRollNumber(),
    issueDate: LocalDate = aValidIssueDate(),
    dateCreated: Instant = aValidRequestDateTime(),
    firstName: String = aValidFirstName(),
    surname: String = aValidSurname(),
    postcode: String = faker.address().postcode(),
): AnonymousElectorDocumentSummary {
    return AnonymousElectorDocumentSummary(
        id = id,
        gssCode = gssCode,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        certificateNumber = certificateNumber,
        electoralRollNumber = electoralRollNumber,
        sanitizedElectoralRollNumber = electoralRollNumber,
        issueDate = issueDate,
        dateCreated = dateCreated,
        firstName = firstName,
        surname = surname,
        sanitizedSurname = surname,
        postcode = postcode
    )
}

fun buildAnonymousElectorDocumentSummaryViewFromAedEntity(
    aedEntity: AnonymousElectorDocument,
    electoralRollNumber: String = aedEntity.electoralRollNumber,
    surname: String = aedEntity.contactDetails!!.surname
): AnonymousElectorDocumentSummary {
    return with(aedEntity) {
        AnonymousElectorDocumentSummary(
            id = id!!,
            gssCode = gssCode,
            sourceType = sourceType,
            sourceReference = sourceReference,
            applicationReference = applicationReference!!,
            certificateNumber = certificateNumber,
            electoralRollNumber = electoralRollNumber,
            sanitizedElectoralRollNumber = electoralRollNumber,
            firstName = contactDetails!!.firstName,
            surname = surname,
            sanitizedSurname = surname,
            postcode = contactDetails!!.address!!.postcode!!,
            issueDate = issueDate,
            dateCreated = requestDateTime
        )
    }
}
