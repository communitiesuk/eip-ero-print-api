package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousElectorDto
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

    @Mapping(target = "dateTime", source = "requestDateTime")
    @Mapping(target = "photoLocation", source = "photoLocationArn")
    abstract fun mapToApiAnonymousElectorDocument(aedSummaryDto: AnonymousElectorDocumentDto): AnonymousElectorDocumentApi

    @Mapping(target = "elector", source = "aedEntity.contactDetails")
    @Mapping(target = "status", constant = "PRINTED")
    @Mapping(target = "deliveryAddressType", source = "aedEntity.delivery.deliveryAddressType")
    @Mapping(target = "collectionReason", source = "aedEntity.delivery.collectionReason")
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
}
