package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity
import uk.gov.dluhc.printapi.dto.DeliveryAddressType as DtoDeliveryAddressType
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType as DeliveryAddressTypeSqs
import uk.gov.dluhc.printapi.models.DeliveryAddressType as ApiDeliveryAddressType

@Mapper
interface DeliveryAddressTypeMapper {

    @ValueMapping(target = "ERO_COLLECTION", source = "ERO_MINUS_COLLECTION")
    fun fromSqsToEntityDeliveryAddressType(sqsDeliveryAddressType: DeliveryAddressTypeSqs): DeliveryAddressTypeEntity

    @ValueMapping(target = "ERO_COLLECTION", source = "ERO_MINUS_COLLECTION")
    fun fromApiToDtoDeliveryAddressType(apiDeliveryAddressType: ApiDeliveryAddressType): DtoDeliveryAddressType

    fun fromDtoToEntityDeliveryAddressType(dtoDeliveryAddressType: DtoDeliveryAddressType): DeliveryAddressTypeEntity
}
