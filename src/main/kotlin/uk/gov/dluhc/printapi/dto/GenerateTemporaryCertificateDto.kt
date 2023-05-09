package uk.gov.dluhc.printapi.dto

import java.time.LocalDate

data class GenerateTemporaryCertificateDto(
    val gssCode: String,
    val sourceType: SourceType,
    val sourceReference: String,
    val applicationReference: String,
    val firstName: String,
    val middleNames: String? = null,
    val surname: String,
    val certificateLanguage: CertificateLanguage,
    val photoLocation: String,
    val validOnDate: LocalDate,
    val userId: String,
)
