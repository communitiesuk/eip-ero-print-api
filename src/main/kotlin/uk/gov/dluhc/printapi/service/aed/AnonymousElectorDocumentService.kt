package uk.gov.dluhc.printapi.service.aed

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.dluhc.printapi.client.ElectoralRegistrationOfficeNotFoundException
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.database.repository.AnonymousElectorDocumentRepository
import uk.gov.dluhc.printapi.dto.PdfFile
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.aed.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.aed.UpdateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.exception.CertificateNotFoundException
import uk.gov.dluhc.printapi.exception.GenerateAnonymousElectorDocumentValidationException
import uk.gov.dluhc.printapi.mapper.aed.AnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.GenerateAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.mapper.aed.ReIssueAnonymousElectorDocumentMapper
import uk.gov.dluhc.printapi.service.EroService
import uk.gov.dluhc.printapi.service.pdf.PdfFactory

@Service
class AnonymousElectorDocumentService(
    private val eroService: EroService,
    private val anonymousElectorDocumentRepository: AnonymousElectorDocumentRepository,
    private val generateAnonymousElectorDocumentMapper: GenerateAnonymousElectorDocumentMapper,
    private val reIssueAnonymousElectorDocumentMapper: ReIssueAnonymousElectorDocumentMapper,
    private val anonymousElectorDocumentMapper: AnonymousElectorDocumentMapper,
    private val pdfTemplateDetailsFactory: AedPdfTemplateDetailsFactory,
    private val pdfFactory: PdfFactory
) {

    companion object {
        private const val PDF_FILE_NAME = "anonymous-elector-document-%s.pdf"
    }

    @Transactional
    fun generateAnonymousElectorDocument(eroId: String, dto: GenerateAnonymousElectorDocumentDto): PdfFile {
        verifyGssCodeIsValidForEro(eroId, dto.gssCode)
        val templateFilename = pdfTemplateDetailsFactory.getTemplateFilename(dto.gssCode)
        with(generateAnonymousElectorDocumentMapper.toAnonymousElectorDocument(dto, templateFilename)) {
            return generatePdf()
                .also { anonymousElectorDocumentRepository.save(this) }
        }
    }

    @Transactional
    fun reIssueAnonymousElectorDocument(eroId: String, dto: ReIssueAnonymousElectorDocumentDto): PdfFile {
        val gssCodes = eroService.lookupGssCodesForEro(eroId)
        val mostRecentAed = getAnonymousElectorDocumentsSortedByDate(gssCodes, dto.sourceReference)
            .firstOrNull() ?: throw CertificateNotFoundException(eroId, ANONYMOUS_ELECTOR_DOCUMENT, dto.sourceReference)

        val templateFilename = pdfTemplateDetailsFactory.getTemplateFilename(mostRecentAed.gssCode)
        with(
            reIssueAnonymousElectorDocumentMapper.toNewAnonymousElectorDocument(
                mostRecentAed,
                dto,
                templateFilename
            )
        ) {
            return generatePdf()
                .also { anonymousElectorDocumentRepository.save(this) }
        }
    }

    @Transactional
    fun updateAnonymousElectorDocument(eroId: String, updateAedDto: UpdateAnonymousElectorDocumentDto) {
        with(updateAedDto) {
            val gssCodes = eroService.lookupGssCodesForEro(eroId)
            val anonymousElectorDocuments = getAnonymousElectorDocumentsSortedByDate(gssCodes, sourceReference)
            if (anonymousElectorDocuments.isEmpty()) {
                throw CertificateNotFoundException(eroId, ANONYMOUS_ELECTOR_DOCUMENT, sourceReference)
            }
            anonymousElectorDocuments.forEach {
                if (valueHasChanged(email, it.contactDetails!!.email)) {
                    it.contactDetails!!.email = email
                }
                if (valueHasChanged(phoneNumber, it.contactDetails!!.phoneNumber)) {
                    it.contactDetails!!.phoneNumber = phoneNumber
                }
            }
        }
    }

    @Transactional(readOnly = true)
    fun getAnonymousElectorDocuments(
        eroId: String,
        applicationId: String
    ): List<AnonymousElectorDocumentDto> {
        val gssCodes = eroService.lookupGssCodesForEro(eroId)
        return getAnonymousElectorDocumentsSortedByDate(gssCodes, applicationId)
            .map { anonymousElectorDocumentMapper.mapToAnonymousElectorDocumentDto(it) }
    }

    @Transactional(readOnly = true)
    fun getAnonymousElectorDocumentsByApplicationId(applicationId: String): List<AnonymousElectorDocumentDto> {
        return anonymousElectorDocumentRepository.findBySourceTypeAndSourceReference(
            sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference = applicationId
        ).map { anonymousElectorDocumentMapper.mapToAnonymousElectorDocumentDto(it) }
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

    private fun getAnonymousElectorDocumentsSortedByDate(
        gssCodes: List<String>,
        sourceReference: String
    ): List<AnonymousElectorDocument> =
        anonymousElectorDocumentRepository.findByGssCodeInAndSourceTypeAndSourceReference(
            gssCodes = gssCodes,
            sourceType = ANONYMOUS_ELECTOR_DOCUMENT,
            sourceReference = sourceReference
        ).sortedByDescending { it.dateCreated }

    private fun AnonymousElectorDocument.generatePdf(): PdfFile {
        val templateDetails = pdfTemplateDetailsFactory.getTemplateDetails(this)
        val contents = pdfFactory.createPdfContents(templateDetails)
        return PdfFile(PDF_FILE_NAME.format(certificateNumber), contents)
    }

    private fun valueHasChanged(newValue: String?, existingValue: String?): Boolean {
        return !newValue.isNullOrBlank() && newValue != existingValue
    }
}
