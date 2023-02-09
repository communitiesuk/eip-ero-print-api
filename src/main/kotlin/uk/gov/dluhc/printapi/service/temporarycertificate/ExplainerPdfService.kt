package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.TemporaryCertificateExplainerDocumentNotFoundException

@Service
class ExplainerPdfService(
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    fun generateExplainerPdf(eroId: String, gssCode: String): PdfFile {
        val eroDetails = getEroOrRaiseNotFoundException(eroId, gssCode)
        val templateDetails = explainerPdfTemplateDetailsFactory.getTemplateDetails(gssCode, eroDetails)
        val contents = pdfFactory.createPdfContents(templateDetails)
        return PdfFile("temporary-certificate-explainer-document-$gssCode.pdf", contents)
    }

    private fun getEroOrRaiseNotFoundException(eroId: String, gssCode: String): EroDto {
        try {
            return eroClient.getEro(gssCode).also {
                if (it.eroId != eroId) {
                    throw TemporaryCertificateExplainerDocumentNotFoundException(eroId, gssCode)
                }
            }
        } catch (error: ElectoralRegistrationOfficeNotFoundException) {
            throw TemporaryCertificateExplainerDocumentNotFoundException(eroId, gssCode)
        }
    }
}
