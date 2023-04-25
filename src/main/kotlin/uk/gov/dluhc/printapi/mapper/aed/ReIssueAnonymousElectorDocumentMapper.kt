package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.models.ReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(
    uses = [
        DeliveryAddressTypeMapper::class,
    ],
    imports = [
        AnonymousElectorDocumentStatus.Status::class,
    ]
)
abstract class ReIssueAnonymousElectorDocumentMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var aedMappingHelper: AedMappingHelper

    abstract fun toReIssueAnonymousElectorDocumentDto(
        apiRequest: ReIssueAnonymousElectorDocumentRequest,
        userId: String
    ): ReIssueAnonymousElectorDocumentDto

    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "issueDate", expression = "java( aedMappingHelper.issueDate() )")
    @Mapping(target = "requestDateTime", expression = "java( aedMappingHelper.requestDateTime() )")
    @Mapping(target = "statusHistory", expression = "java( aedMappingHelper.statusHistory(Status.PRINTED) )")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "delivery.id", ignore = true)
    @Mapping(target = "delivery.address.id", ignore = true)
    @Mapping(target = "contactDetails.id", ignore = true)
    @Mapping(target = "contactDetails.address.id", ignore = true)
    abstract fun toNewAnonymousElectorDocument(
        previousAed: AnonymousElectorDocument,
        aedTemplateFilename: String
    ): AnonymousElectorDocument
}
