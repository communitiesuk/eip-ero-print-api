package uk.gov.dluhc.printapi.service.pdf

import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.ExplainerDocumentNotFoundException

class ExplainerPdfService(
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val explainerPdfTemplateDetailsFactory: ExplainerPdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    fun generateExplainerPdf(eroId: String, gssCode: String): PdfFile {
        val eroDetails = getEroOrRaiseNotFoundException(eroId, gssCode)
        val templateDetails = explainerPdfTemplateDetailsFactory.getTemplateDetails(gssCode, eroDetails)
        val contents = pdfFactory.createPdfContents(templateDetails)
        return PdfFile("${explainerPdfTemplateDetailsFactory.getDownloadFilenamePrefix()}-$gssCode.pdf", contents)
    }

    private fun getEroOrRaiseNotFoundException(eroId: String, gssCode: String): EroDto {
        try {
            return eroClient.getEro(gssCode).also {
                if (it.eroId != eroId) {
                    throw ExplainerDocumentNotFoundException(
                        explainerPdfTemplateDetailsFactory.getExceptionMessage(eroId, gssCode)
                    )
                }
            }
        } catch (error: ElectoralRegistrationOfficeNotFoundException) {
            throw ExplainerDocumentNotFoundException(
                explainerPdfTemplateDetailsFactory.getExceptionMessage(eroId, gssCode)
            )
        }
    }
}
