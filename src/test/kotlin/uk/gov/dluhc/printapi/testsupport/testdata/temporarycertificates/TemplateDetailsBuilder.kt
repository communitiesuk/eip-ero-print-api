package uk.gov.dluhc.printapi.testsupport.testdata.temporarycertificates

import uk.gov.dluhc.printapi.service.pdf.TemplateDetails
import uk.gov.dluhc.printapi.testsupport.testdata.aGssCode
import uk.gov.dluhc.printapi.testsupport.testdata.aValidEroName
import uk.gov.dluhc.printapi.testsupport.testdata.aValidVacNumber

fun buildTemplateDetails(
    path: String = aTemplatePath(),
    placeholders: Map<String, String> = buildTemplatePlaceholders()
): TemplateDetails =
    TemplateDetails(
        path = path,
        placeholders = placeholders
    )

fun aTemplateFilename() = "Temp Voter Authority Certificate (English) v1.pdf"

fun aTemplatePath(
    templateFilename: String = aTemplateFilename()
) = "classpath:temporary-certificate-template/$templateFilename"

fun buildTemplatePlaceholders() = mapOf(
    "elector-name" to "John Smith",
    "gss-code" to aGssCode(),
    "ero-name" to aValidEroName(),
    "certificate-number" to aValidVacNumber()
)
