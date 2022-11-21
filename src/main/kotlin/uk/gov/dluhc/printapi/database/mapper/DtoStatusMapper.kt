package uk.gov.dluhc.printapi.database.mapper

import uk.gov.dluhc.printapi.database.entity.Status
import uk.gov.dluhc.printapi.dto.StatusDto

class DtoStatusMapper {
    fun toDtoStatus(status: Status): StatusDto {
        return StatusDto.valueOf(status.name)
    }
}
