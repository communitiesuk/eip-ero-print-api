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
    @ValueMapping(source = "ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT", target = "ANONYMOUS_ELECTOR_DOCUMENT")
    fun mapSqsToEntity(sourceType: SourceTypeSqsModel): SourceTypeEntity

    @ValueMapping(source = "VOTER_MINUS_CARD", target = "VOTER_CARD")
    @ValueMapping(source = "ANONYMOUS_MINUS_ELECTOR_MINUS_DOCUMENT", target = "ANONYMOUS_ELECTOR_DOCUMENT")
    fun mapApiToDto(sourceType: SourceTypeApi): SourceTypeDto

    fun mapDtoToEntity(sourceType: SourceTypeDto): SourceTypeEntity
}
