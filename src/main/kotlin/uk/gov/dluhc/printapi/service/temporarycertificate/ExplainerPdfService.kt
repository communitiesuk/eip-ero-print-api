package uk.gov.dluhc.printapi.service.temporarycertificate

import org.springframework.stereotype.Service
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.dto.PdfFile

@Service
class ExplainerPdfService(
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val explainerPdfFactory: ExplainerPdfFactory
) {

    fun generateExplainerPdf(gssCode: String): PdfFile {
        val eroDto = eroClient.getEro(gssCode)
        val contents = explainerPdfFactory.createPdfContents(eroDto, gssCode)
        return PdfFile("temporary-certificate-explainer-document-$gssCode.pdf", contents)
    }
}