package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.dto.SourceType as SourceTypeDto
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeSqsModel
import uk.gov.dluhc.printapi.models.SourceType as SourceTypeApi

@Mapper
interface SourceTypeMapper {
    @ValueMapping(source = "VOTER_MINUS_CARD", target = "VOTER_CARD")
    fun toSourceTypeEntity(sourceType: SourceTypeSqsModel): SourceTypeEntity

    @ValueMapping(source = "VOTER_MINUS_CARD", target = "VOTER_CARD")
    fun toSourceTypeDto(sourceType: SourceTypeApi): SourceTypeDto
}
