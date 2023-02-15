package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateMapper
import uk.gov.dluhc.printapi.service.pdf.ElectorDocumentPdfTemplateDetailsFactory
import uk.gov.dluhc.printapi.service.pdf.PdfFactory
import uk.gov.dluhc.printapi.validator.service.GenerateTemporaryCertificateValidator

@Service
class TemporaryCertificateService(
    private val validator: GenerateTemporaryCertificateValidator,
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val temporaryCertificateRepository: TemporaryCertificateRepository,
    private val temporaryCertificateMapper: TemporaryCertificateMapper,
    @Qualifier("temporaryCertificateElectorDocumentPdfTemplateDetailsFactory") private val electorDocumentPdfTemplateDetailsFactory: ElectorDocumentPdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    /**
     * Generates a Temporary Certificate
     * @throws GenerateTemporaryCertificateValidationException if the GenerateTemporaryCertificateDto fails validation
     */
    @Transactional
    fun generateTemporaryCertificate(eroId: String, request: GenerateTemporaryCertificateDto): PdfFile {
        validator.validate(request)
        val eroDetails = getEroOrRaiseValidationException(eroId, request.gssCode)
        val filename = electorDocumentPdfTemplateDetailsFactory.getTemplateFilename(request.gssCode)
        val temporaryCertificate = temporaryCertificateMapper.toTemporaryCertificate(request, eroDetails, filename)
        val templateDetails = electorDocumentPdfTemplateDetailsFactory.getTemplateDetails(temporaryCertificate)
        val contents = pdfFactory.createPdfContents(templateDetails)
        temporaryCertificateRepository.save(temporaryCertificate)
        return PdfFile("temporary-certificate-${temporaryCertificate.certificateNumber}.pdf", contents)
    }

    private fun getEroOrRaiseValidationException(eroId: String, gssCode: String): EroDto {
        try {
            return eroClient.getEro(gssCode).also {
                if (it.eroId != eroId) {
                    throw GenerateTemporaryCertificateValidationException("Temporary Certificate gssCode '$gssCode' is not valid for eroId '$eroId'")
                }
            }
        } catch (error: ElectoralRegistrationOfficeNotFoundException) {
            throw GenerateTemporaryCertificateValidationException("Temporary Certificate gssCode '$gssCode' does not exist")
        }
    }
}
