package uk.gov.dluhc.printapi.service.aed

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeManagementApiClient
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.GenerateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.mapper.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.service.pdf.PdfFactory

@Service
class AnonymousElectorDocumentService(
    private val eroClient: ElectoralRegistrationOfficeManagementApiClient,
    private val anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository,
    private val anonymousElectorDocumentMapper: AnonymousElectorDocumentMapper,
    private val pdfTemplateDetailsFactory: AedPdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    @Transactional
    fun generateAnonymousElectorDocument(eroId: String, request: GenerateAnonymousElectorDocumentDto): PdfFile {
        verifyGssCodeIsValidForEro(eroId, request.gssCode)
        val filename = pdfTemplateDetailsFactory.getTemplateFilename(request.gssCode)
        val electorDocument = anonymousElectorDocumentMapper.toAnonymousElectorDocument(request, filename)
        val templateDetails = pdfTemplateDetailsFactory.getTemplateDetails(electorDocument)
        val contents = pdfFactory.createPdfContents(templateDetails)
        anonymousElectorDocumentRepository.save(electorDocument)
        return PdfFile("anonymous-elector-document-${electorDocument.certificateNumber}.pdf", contents)
    }

    private fun verifyGssCodeIsValidForEro(eroId: String, gssCode: String) {
        try {
            eroClient.getEro(gssCode).also {
                if (it.eroId != eroId) {
                    throw GenerateAnonymousElectorDocumentValidationException("Anonymous Elector Document gssCode '$gssCode' is not valid for eroId '$eroId'")
                }
            }
        } catch (error: ElectoralRegistrationOfficeNotFoundException) {
            throw GenerateAnonymousElectorDocumentValidationException("Anonymous Elector Document gssCode '$gssCode' does not exist")
        }
    }
}
