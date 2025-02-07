package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.NullValueCheckStrategy
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.dto.SourceType.ANONYMOUS_ELECTOR_DOCUMENT
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentStatus
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDto
import uk.gov.dluhc.printapi.factory.UrlFactory
import uk.gov.dluhc.printapi.mapper.CertificateLanguageMapper
import uk.gov.dluhc.printapi.mapper.DeliveryAddressTypeMapper
import uk.gov.dluhc.printapi.mapper.InstantMapper
import uk.gov.dluhc.printapi.models.AnonymousElectorDocument as AnonymousElectorDocumentApi

@Mapper(
    uses = [
        AnonymousSupportingInformationFormatMapper::class,
        CertificateLanguageMapper::class,
        DeliveryAddressTypeMapper::class,
        InstantMapper::class
    ]
)
abstract class AnonymousElectorDocumentMapper {

    @Autowired
    protected lateinit var urlFactory: UrlFactory

    @Mapping(target = "dateTime", source = "dto.requestDateTime")
    @Mapping(target = "photoUrl", expression = "java(getPhotoUrl(eroId, dto))")
    abstract fun mapToApiAnonymousElectorDocument(dto: AnonymousElectorDocumentDto, eroId: String): AnonymousElectorDocumentApi

    @Mapping(target = "elector", source = "aedEntity.contactDetails")
    @Mapping(target = "status", expression = "java(getAedStatusByInitialRetentionDataRemoved(aedEntity))")
    @Mapping(target = "deliveryAddressType", source = "aedEntity.delivery.deliveryAddressType", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "collectionReason", source = "aedEntity.delivery.collectionReason")
    @Mapping(target = "supportingInformationFormat", source = "aedEntity.supportingInformationFormat", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    abstract fun mapToAnonymousElectorDocumentDto(aedEntity: AnonymousElectorDocument): AnonymousElectorDocumentDto

    @Mapping(target = "addressee", expression = "java(getAssigneeName(aedContactDetailsEntity))")
    @Mapping(target = "registeredAddress", source = "aedContactDetailsEntity.address")
    protected abstract fun mapFromContactDetailsToElectorDto(aedContactDetailsEntity: AedContactDetails): AnonymousElectorDto

    protected fun getAssigneeName(aedContactDetailsEntity: AedContactDetails): String {
        return with(aedContactDetailsEntity) {
            if (middleNames.isNullOrBlank()) {
                "$firstName $surname"
            } else {
                "$firstName $middleNames $surname"
            }
        }
    }

    protected fun getPhotoUrl(eroId: String, dto: AnonymousElectorDocumentDto): String =
        urlFactory.createPhotoUrl(eroId, ANONYMOUS_ELECTOR_DOCUMENT, dto.sourceReference)

    protected fun getAedStatusByInitialRetentionDataRemoved(aedEntity: AnonymousElectorDocument): AnonymousElectorDocumentStatus =
        if (aedEntity.initialRetentionDataRemoved) {
            AnonymousElectorDocumentStatus.EXPIRED
        } else {
            AnonymousElectorDocumentStatus.PRINTED
        }
}
