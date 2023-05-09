package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.dto.TemporaryCertificateSummaryDto
import uk.gov.dluhc.printapi.models.TemporaryCertificateSummary

@Mapper(uses = [InstantMapper::class])
interface TemporaryCertificateSummaryMapper {

    fun toApiTemporaryCertificateSummary(dto: TemporaryCertificateSummaryDto): TemporaryCertificateSummary

    fun toDtoTemporaryCertificateSummary(entity: TemporaryCertificate): TemporaryCertificateSummaryDto
}
