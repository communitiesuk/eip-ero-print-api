package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateMapper
import uk.gov.dluhc.printapi.validator.service.GenerateTemporaryCertificateValidator

@Service
class TemporaryCertificateService(
    private val validator: GenerateTemporaryCertificateValidator,
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val temporaryCertificateRepository: TemporaryCertificateRepository,
    private val temporaryCertificateMapper: TemporaryCertificateMapper,
    private val certificatePdfTemplateDetailsFactory: CertificatePdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    /**
     * Generates a Temporary Certificate
     * @throws GenerateTemporaryCertificateValidationException if the GenerateTemporaryCertificateDto fails validation
     */
    @Transactional
    fun generateTemporaryCertificate(request: GenerateTemporaryCertificateDto): PdfFile {
        validator.validate(request)
        val eroDetails = eroClient.getEro(request.gssCode)
        val filename = certificatePdfTemplateDetailsFactory.getTemplateFilename(request.gssCode)
        val temporaryCertificate = temporaryCertificateMapper.toTemporaryCertificate(request, eroDetails, filename)
        val templateDetails = certificatePdfTemplateDetailsFactory.getTemplateDetails(temporaryCertificate)
        temporaryCertificateRepository.save(temporaryCertificate)
        val contents = pdfFactory.createPdfContents(templateDetails)
        return PdfFile("temporary-certificate-${temporaryCertificate.certificateNumber}.pdf", contents)
    }
}
