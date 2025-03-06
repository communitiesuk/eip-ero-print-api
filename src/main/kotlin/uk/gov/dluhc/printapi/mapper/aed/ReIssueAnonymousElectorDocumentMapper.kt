package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentDelivery
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.aed.ReIssueAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.models.ReIssueAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.service.ElectorDocumentRemovalDateResolver
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

    @Autowired
    protected lateinit var deliveryAddressTypeMapper: DeliveryAddressTypeMapper

    @Autowired
    protected lateinit var removalDateResolver: ElectorDocumentRemovalDateResolver

    abstract fun toReIssueAnonymousElectorDocumentDto(
        apiRequest: ReIssueAnonymousElectorDocumentRequest,
        userId: String
    ): ReIssueAnonymousElectorDocumentDto

    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "issueDate", expression = "java( aedMappingHelper.issueDate() )")
    @Mapping(target = "requestDateTime", expression = "java( aedMappingHelper.requestDateTime() )")
    @Mapping(target = "statusHistory", expression = "java( aedMappingHelper.statusHistory(Status.PRINTED) )")
    @Mapping(target = "sourceReference", source = "previousAed.sourceReference")
    @Mapping(target = "electoralRollNumber", source = "dto.electoralRollNumber")
    @Mapping(target = "userId", source = "dto.userId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreated", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "delivery.id", ignore = true)
    @Mapping(target = "delivery.dateCreated", ignore = true)
    @Mapping(target = "delivery.createdBy", ignore = true)
    @Mapping(target = "delivery.version", ignore = true)
    @Mapping(target = "delivery.address.id", ignore = true)
    @Mapping(target = "delivery.address.dateCreated", ignore = true)
    @Mapping(target = "delivery.address.createdBy", ignore = true)
    @Mapping(target = "delivery.address.version", ignore = true)
    @Mapping(target = "contactDetails.id", ignore = true)
    @Mapping(target = "contactDetails.dateCreated", ignore = true)
    @Mapping(target = "contactDetails.createdBy", ignore = true)
    @Mapping(target = "contactDetails.dateUpdated", ignore = true)
    @Mapping(target = "contactDetails.updatedBy", ignore = true)
    @Mapping(target = "contactDetails.version", ignore = true)
    @Mapping(target = "contactDetails.address.id", ignore = true)
    @Mapping(target = "contactDetails.address.dateCreated", ignore = true)
    @Mapping(target = "contactDetails.address.createdBy", ignore = true)
    @Mapping(target = "contactDetails.address.version", ignore = true)
    @Mapping(target = "initialRetentionDataRemoved", ignore = true)
    abstract fun toNewAnonymousElectorDocument(
        previousAed: AnonymousElectorDocument,
        dto: ReIssueAnonymousElectorDocumentDto,
        aedTemplateFilename: String
    ): AnonymousElectorDocument

    @AfterMapping
    protected fun setDeliveryAddressTypeInNewAnonymousElectorDocument(
        @MappingTarget aed: AnonymousElectorDocument,
        dto: ReIssueAnonymousElectorDocumentDto,
    ) {
        val deliveryAddressTypeEntity = deliveryAddressTypeMapper.mapDtoToEntity(dto.deliveryAddressType)

        if (aed.delivery != null) {
            aed.delivery?.deliveryAddressType = deliveryAddressTypeEntity
        } else {
            aed.delivery = AnonymousElectorDocumentDelivery(deliveryAddressType = deliveryAddressTypeEntity)
        }
    }

    @AfterMapping
    protected fun setDataRetentionDates(
        @MappingTarget aed: AnonymousElectorDocument,
        previousAed: AnonymousElectorDocument
    ) {
        if (previousAed.initialRetentionRemovalDate != null) {
            aed.initialRetentionRemovalDate = removalDateResolver.getAedInitialRetentionPeriodRemovalDate(aed.issueDate)
        }

        if (previousAed.finalRetentionRemovalDate != null) {
            aed.finalRetentionRemovalDate = removalDateResolver.getElectorDocumentFinalRetentionPeriodRemovalDate(aed.issueDate)
        }
    }
}
