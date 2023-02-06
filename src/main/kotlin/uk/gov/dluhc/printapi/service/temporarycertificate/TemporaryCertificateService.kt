package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import uk.gov.dluhc.printapi.validator.service.GenerateTemporaryCertificateValidator

@Service
class TemporaryCertificateService(private val validator: GenerateTemporaryCertificateValidator) {

    /**
     * Generates a Temporary Certificate
     * @throws GenerateTemporaryCertificateValidationException if the GenerateTemporaryCertificateDto fails validation
     */
    fun generateTemporaryCertificate(generateTemporaryCertificateDto: GenerateTemporaryCertificateDto): PdfFile {
        validator.validate(generateTemporaryCertificateDto)

        TODO("not yet implemented")
    }
}
