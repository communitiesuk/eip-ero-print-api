package uk.gov.dluhc.printapi.mapper

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DtoDeliveryAddressType
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType as DeliveryAddressTypeSqs
import uk.gov.dluhc.printapi.models.DeliveryAddressType as ApiDeliveryAddressType

@Mapper
interface DeliveryAddressTypeMapper {

    fun mapSqsToEntity(sqsType: DeliveryAddressTypeSqs): DeliveryAddressTypeEntity

    fun mapEntityToApi(entityType: DeliveryAddressTypeEntity): ApiDeliveryAddressType

    fun mapApiToDto(apiType: ApiDeliveryAddressType): DtoDeliveryAddressType

    @InheritInverseConfiguration
    fun mapDtoToApi(dtoType: DtoDeliveryAddressType): ApiDeliveryAddressType

    fun mapDtoToEntity(dtoType: DtoDeliveryAddressType): DeliveryAddressTypeEntity

    @InheritInverseConfiguration
    fun mapEntityToDto(entityType: DeliveryAddressTypeEntity): DtoDeliveryAddressType
}
