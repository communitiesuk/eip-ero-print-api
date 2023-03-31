package uk.gov.dluhc.printapi.service.aed

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.dto.AnonymousElectorDocumentSummaryDto
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.exception.GenerateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.mapper.aed.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.AnonymousElectorSummaryMapper
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.service.pdf.PdfFactory

@Service
class AnonymousElectorDocumentService(
    private val eroService: EroService,
    private val anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository,
    private val anonymousElectorDocumentMapper: AnonymousElectorDocumentMapper,
    private val anonymousElectorSummaryMapper: AnonymousElectorSummaryMapper,
    private val pdfTemplateDetailsFactory: AedPdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    @Transactional
    fun generateAnonymousElectorDocument(eroId: String, dto: GenerateAnonymousElectorDocumentDto): PdfFile {
        verifyGssCodeIsValidForEro(eroId, dto.gssCode)
        val filename = pdfTemplateDetailsFactory.getTemplateFilename(dto.gssCode)
        with(anonymousElectorDocumentMapper.toAnonymousElectorDocument(dto, filename)) {
            val templateDetails = pdfTemplateDetailsFactory.getTemplateDetails(this)
            val contents = pdfFactory.createPdfContents(templateDetails)
            anonymousElectorDocumentRepository.save(this)
            return PdfFile("anonymous-elector-document-$certificateNumber.pdf", contents)
        }
    }

    fun getAnonymousElectorDocumentSummaries(
        eroId: String,
        applicationId: String
    ): List<AnonymousElectorDocumentSummaryDto> {
        val gssCodes = eroService.lookupGssCodesForEro(eroId)
        return anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(
            gssCodes = gssCodes,
            sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference = applicationId
        ).sortedByDescending { it.dateCreated }
            .map { anonymousElectorSummaryMapper.mapToAnonymousElectorDocumentSummaryDto(it) }
    }

    private fun verifyGssCodeIsValidForEro(eroId: String, gssCode: String) {
        try {
            if (!eroService.isGssCodeValidForEro(gssCode = gssCode, eroIdToMatch = eroId)) {
                throw GenerateAnonymousElectorDocumentValidationException("Anonymous Elector Document gssCode '$gssCode' is not valid for eroId '$eroId'")
            }
        } catch (error: ElectoralRegistrationOfficeNotFoundException) {
            throw GenerateAnonymousElectorDocumentValidationException("Anonymous Elector Document gssCode '$gssCode' does not exist")
        }
    }
}
