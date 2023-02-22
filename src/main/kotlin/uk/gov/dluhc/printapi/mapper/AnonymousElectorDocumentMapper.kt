package uk.gov.dluhc.printapi.mapper

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(
    uses = [
        SourceTypeMapper::class,
        CertificateLanguageMapper::class,
        SupportingInformationFormatMapper::class
    ]
)
abstract class AnonymousElectorDocumentMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var printRequestMapper: AedPrintRequestMapper

    @Mapping(target = "photoLocationArn", source = "aedRequest.photoLocation")
    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "printRequests", expression = "java( kotlin.collections.CollectionsKt.mutableListOf() )")
    @Mapping(target = "contactDetails", source = "aedRequest")
    abstract fun toAnonymousElectorDocument(
        aedRequest: GenerateAnonymousElectorDocumentDto,
        aedTemplateFilename: String
    ): AnonymousElectorDocument

    @AfterMapping
    protected fun addPrintRequestToAnonymousElectoralDocument(
        aedRequest: GenerateAnonymousElectorDocumentDto,
        aedTemplateFilename: String,
        @MappingTarget anonymousElectorDocument: AnonymousElectorDocument
    ) {
        anonymousElectorDocument.addPrintRequest(printRequestMapper.toPrintRequest(aedRequest, aedTemplateFilename))
    }
}
