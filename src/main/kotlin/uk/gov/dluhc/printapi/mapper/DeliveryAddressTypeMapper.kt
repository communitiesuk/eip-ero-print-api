package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.messaging.models.DeliveryAddressType
import uk.gov.dluhc.printapi.database.entity.DeliveryAddressType as DeliveryAddressTypeEntity

@Mapper
interface DeliveryAddressTypeMapper {

    @ValueMapping(source = "ERO_MINUS_COLLECTION", target = "ERO_COLLECTION")
    fun fromSqsToDeliveryAddressTypeEntity(sourceType: DeliveryAddressType): DeliveryAddressTypeEntity
}
