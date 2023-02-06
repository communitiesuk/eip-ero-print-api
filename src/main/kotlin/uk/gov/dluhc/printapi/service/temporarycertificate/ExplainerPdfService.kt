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
    private val explainerPdfFactory: ExplainerPdfFactory
) {

    fun generateExplainerPdf(gssCode: String): PdfFile {
        val eroDto = getEroOrRaiseNotFoundException(gssCode)
        val contents = explainerPdfFactory.createPdfContents(eroDto, gssCode)
        return PdfFile("temporary-certificate-explainer-document-$gssCode.pdf", contents)
    }

    private fun getEroOrRaiseNotFoundException(gssCode: String): EroDto {
        try {
            return eroClient.getEro(gssCode)
        } catch (error: ElectoralRegistrationOfficeNotFoundException) {
            throw TemporaryCertificateExplainerDocumentNotFoundException(gssCode)
        }
    }
}
