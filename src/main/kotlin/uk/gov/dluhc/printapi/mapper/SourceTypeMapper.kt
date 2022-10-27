package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeModel

@Mapper
interface SourceTypeMapper {
    @ValueMapping(source = "VOTER_MINUS_CARD", target = "VOTER_CARD")
    fun toSourceTypeEntity(sourceType: SourceTypeModel): SourceTypeEntity
}
