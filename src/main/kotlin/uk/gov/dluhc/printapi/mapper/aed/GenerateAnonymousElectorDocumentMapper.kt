package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentDelivery
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.CertificateDelivery
import uk.gov.dluhc.printapi.dto.aed.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.mapper.CertificateLanguageMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(
    uses = [
        AnonymousSupportingInformationFormatMapper::class,
        CertificateLanguageMapper::class,
        DeliveryAddressTypeMapper::class,
        SourceTypeMapper::class,
    ],
    imports = [
        AnonymousElectorDocumentStatus.Status::class,
    ]
)
abstract class GenerateAnonymousElectorDocumentMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var aedMappingHelper: AedMappingHelper

    abstract fun toGenerateAnonymousElectorDocumentDto(
        apiRequest: GenerateAnonymousElectorDocumentRequest,
        userId: String
    ): GenerateAnonymousElectorDocumentDto

    @Mapping(target = "photoLocationArn", source = "aedDto.photoLocation")
    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(target = "issueDate", expression = "java( aedMappingHelper.issueDate() )")
    @Mapping(target = "requestDateTime", expression = "java( aedMappingHelper.requestDateTime() )")
    @Mapping(target = "contactDetails", source = "aedDto")
    @Mapping(target = "statusHistory", expression = "java( aedMappingHelper.statusHistory(Status.PRINTED) )")
    @Mapping(target = "delivery", source = "aedDto.delivery")
    abstract fun toAnonymousElectorDocument(
        aedDto: GenerateAnonymousElectorDocumentDto,
        aedTemplateFilename: String
    ): AnonymousElectorDocument

    @Mapping(target = "address", source = "registeredAddress")
    protected abstract fun toAedContactDetailsEntity(aedDto: GenerateAnonymousElectorDocumentDto): AedContactDetails

    @Mapping(target = "address", source = "deliveryAddress")
    protected abstract fun fromDeliveryDtoToAnonymousElectorDocumentDeliveryEntity(deliveryDto: CertificateDelivery): AnonymousElectorDocumentDelivery
}
