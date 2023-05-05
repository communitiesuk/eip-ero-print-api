package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryDto
import uk.gov.dluhc.printapi.dto.aed.AnonymousSearchSummaryResults
import uk.gov.dluhc.printapi.mapper.InstantMapper
import uk.gov.dluhc.printapi.models.AedSearchSummaryResponse
import uk.gov.dluhc.printapi.database.entity.AnonymousElectorDocumentSummary as AnonymousElectorDocumentSummaryEntity
import uk.gov.dluhc.printapi.models.AedSearchSummary as AedSearchSummaryApi

@Mapper(uses = [InstantMapper::class])
abstract class AnonymousSearchSummaryMapper {

    @Mapping(target = "dateTimeCreated", source = "dateCreated")
    abstract fun toAnonymousSearchSummaryDto(entity: AnonymousElectorDocumentSummaryEntity): AnonymousSearchSummaryDto

    abstract fun toAedSearchSummaryResponse(aedSummaryResultsDto: AnonymousSearchSummaryResults): AedSearchSummaryResponse

    @Mapping(target = "status", constant = "PRINTED")
    protected abstract fun toAedSearchSummaryApi(dto: AnonymousSearchSummaryDto): AedSearchSummaryApi
}
