package uk.gov.dluhc.printapi.testsupport.testdata.model

import uk.gov.dluhc.printapi.models.CertificateLanguage
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest
import uk.gov.dluhc.printapi.models.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.LocalDate

fun buildGenerateTemporaryCertificateRequest(
    gssCode: String = aGssCode(),
    sourceType: SourceType = SourceType.VOTER_MINUS_CARD,
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    certificateLanguage: CertificateLanguage = CertificateLanguage.EN,
    photoLocation: String = aPhotoArn(),
    validOnDate: LocalDate = aValidIssueDate(),
): GenerateTemporaryCertificateRequest =
    GenerateTemporaryCertificateRequest(
        gssCode = gssCode,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        firstName = firstName,
        middleNames = middleNames,
        surname = surname,
        certificateLanguage = certificateLanguage,
        photoLocation = photoLocation,
        validOnDate = validOnDate
    )
