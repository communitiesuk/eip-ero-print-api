package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.repository.TemporaryCertificateRepository
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.exception.GenerateTemporaryCertificateValidationException
import uk.gov.dluhc.printapi.mapper.TemporaryCertificateMapper
import uk.gov.dluhc.printapi.service.S3AccessService
import uk.gov.dluhc.printapi.service.pdf.PdfFactory
import uk.gov.dluhc.printapi.service.pdf.TemporaryCertificatePdfTemplateDetailsFactory
import uk.gov.dluhc.printapi.validator.service.GenerateTemporaryCertificateValidator
import java.net.URI

@Service
class TemporaryCertificateService(
    private val validator: GenerateTemporaryCertificateValidator,
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val temporaryCertificateRepository: TemporaryCertificateRepository,
    private val temporaryCertificateMapper: TemporaryCertificateMapper,
    private val temporaryCertificatePdfTemplateDetailsFactory: TemporaryCertificatePdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory,
    private val s3AccessService: S3AccessService,
) {

    /**
     * Generates a Temporary Certificate, stores in S3 and returns a pre-signed URL to access the certificate
     * @throws GenerateTemporaryCertificateValidationException if the GenerateTemporaryCertificateDto fails validation
     */
    @Transactional
    fun generateTemporaryCertificate(eroId: String, request: GenerateTemporaryCertificateDto): URI {
        validator.validate(request)
        val eroDetails = getEroOrRaiseValidationException(eroId, request.gssCode)
        val templateFilename = temporaryCertificatePdfTemplateDetailsFactory.getTemplateFilename(request.gssCode)
        val temporaryCertificate =
            temporaryCertificateMapper.toTemporaryCertificate(request, eroDetails, templateFilename)
        val templateDetails = temporaryCertificatePdfTemplateDetailsFactory.getTemplateDetails(temporaryCertificate)
        val contents = pdfFactory.createPdfContents(templateDetails)
        val fileName = "temporary-certificate-${temporaryCertificate.certificateNumber}.pdf"
        val presignedUrl = s3AccessService.uploadTemporaryCertificate(
            gssCode = request.gssCode,
            applicationId = request.sourceReference,
            fileName = fileName,
            contents = contents,
        )
        temporaryCertificateRepository.save(temporaryCertificate)
        return presignedUrl
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
