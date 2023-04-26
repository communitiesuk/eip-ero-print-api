package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.mapper.InstantMapper
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary as AnonymousElectorDocumentSummaryEntity
import uk.gov.dluhc.printapi.models.AedSearchSummary as AedSearchSummaryApi

@Mapper(uses = [InstantMapper::class])
interface AnonymousSearchSummaryMapper {

    @Mapping(target = "dateTimeCreated", source = "dateCreated")
    fun toAnonymousSearchSummaryDto(entity: AnonymousElectorDocumentSummaryEntity): AnonymousSearchSummaryDto

    @Mapping(target = "status", constant = "PRINTED")
    fun toAnonymousSearchSummaryApi(dto: AnonymousSearchSummaryDto): AedSearchSummaryApi
}
