package uk.gov.dluhc.printapi.testsupport.testdata.dto

import uk.gov.dluhc.printapi.dto.CertificateLanguage
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.dto.SourceType
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidApplicationReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidCertificateLanguageDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidFirstName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidIssueDate
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceReference
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSourceTypeDto
import uk.gov.dluhc.printapi.testsupport.testdata.aValidSurname
import uk.gov.dluhc.printapi.testsupport.testdata.aValidUserId
import uk.gov.dluhc.printapi.testsupport.testdata.zip.aPhotoArn
import java.time.LocalDate

fun buildGenerateTemporaryCertificateDto(
    gssCode: String = aGssCode(),
    sourceType: SourceType = aValidSourceTypeDto(),
    sourceReference: String = aValidSourceReference(),
    applicationReference: String = aValidApplicationReference(),
    firstName: String = aValidFirstName(),
    middleNames: String? = null,
    surname: String = aValidSurname(),
    certificateLanguage: CertificateLanguage = aValidCertificateLanguageDto(),
    photoLocation: String = aPhotoArn(),
    validOnDate: LocalDate = aValidIssueDate(),
    userId: String = aValidUserId(),
): GenerateTemporaryCertificateDto =
    GenerateTemporaryCertificateDto(
        gssCode = gssCode,
        sourceType = sourceType,
        sourceReference = sourceReference,
        applicationReference = applicationReference,
        firstName = firstName,
        middleNames = middleNames,
        surname = surname,
        certificateLanguage = certificateLanguage,
        photoLocation = photoLocation,
        validOnDate = validOnDate,
        userId = userId
    )
