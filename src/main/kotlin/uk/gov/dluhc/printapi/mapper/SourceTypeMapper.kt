package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.database.entity.SourceType as SourceTypeEntity
import uk.gov.dluhc.printapi.dto.SourceType as SourceTypeDto
import uk.gov.dluhc.printapi.messaging.models.SourceType as SourceTypeSqsModel
import uk.gov.dluhc.printapi.models.SourceType as SourceTypeApi

@Mapper
interface SourceTypeMapper {
    fun mapSqsToEntity(sourceType: SourceTypeSqsModel): SourceTypeEntity

    fun mapApiToDto(sourceType: SourceTypeApi): SourceTypeDto

    fun mapDtoToEntity(sourceType: SourceTypeDto): SourceTypeEntity
}
