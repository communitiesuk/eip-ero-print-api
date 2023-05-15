package uk.gov.dluhc.printapi.mapper

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DtoDeliveryAddressType
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType as DeliveryAddressTypeSqs
import uk.gov.dluhc.printapi.models.DeliveryAddressType as ApiDeliveryAddressType

@Mapper
interface DeliveryAddressTypeMapper {

    @ValueMapping(target = "ERO_COLLECTION", source = "ERO_MINUS_COLLECTION")
    fun mapSqsToEntity(sqsType: DeliveryAddressTypeSqs): DeliveryAddressTypeEntity

    @ValueMapping(target = "ERO_MINUS_COLLECTION", source = "ERO_COLLECTION")
    fun mapEntityToApi(entityType: DeliveryAddressTypeEntity): ApiDeliveryAddressType

    @ValueMapping(target = "ERO_COLLECTION", source = "ERO_MINUS_COLLECTION")
    fun mapApiToDto(apiType: ApiDeliveryAddressType): DtoDeliveryAddressType

    @InheritInverseConfiguration
    fun mapDtoToApi(dtoType: DtoDeliveryAddressType): ApiDeliveryAddressType

    fun mapDtoToEntity(dtoType: DtoDeliveryAddressType): DeliveryAddressTypeEntity

    @InheritInverseConfiguration
    fun mapEntityToDto(entityType: DeliveryAddressTypeEntity): DtoDeliveryAddressType
}
