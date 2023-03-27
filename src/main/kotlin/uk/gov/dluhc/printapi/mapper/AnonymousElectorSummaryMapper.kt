package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.AedContactDetails
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocument
import uk.gov.dluhc.printapi.dto.AnonymousElectorDocumentSummaryDto
import uk.gov.dluhc.printapi.dto.AnonymousElectorDto
import uk.gov.dluhc.printapi.models.AnonymousElectorDocumentSummary

@Mapper(
    uses = [
        CertificateLanguageMapper::class,
        SupportingInformationFormatMapper::class,
        DeliveryAddressTypeMapper::class,
        InstantMapper::class
    ]
)
abstract class AnonymousElectorSummaryMapper {

    @Mapping(target = "dateTime", source = "requestDateTime")
    @Mapping(target = "photoLocation", source = "photoLocationArn")
    abstract fun mapToApiAnonymousElectorDocumentSummary(aedSummaryDto: AnonymousElectorDocumentSummaryDto): AnonymousElectorDocumentSummary

    @Mapping(target = "elector", source = "aedEntity.contactDetails")
    @Mapping(target = "status", constant = "PRINTED")
    @Mapping(target = "deliveryAddressType", source = "aedEntity.delivery.deliveryAddressType")
    abstract fun mapToAnonymousElectorDocumentSummaryDto(aedEntity: AnonymousElectorDocument): AnonymousElectorDocumentSummaryDto

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
