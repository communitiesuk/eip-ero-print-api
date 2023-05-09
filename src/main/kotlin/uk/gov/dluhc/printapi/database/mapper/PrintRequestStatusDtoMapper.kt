package uk.gov.dluhc.printapi.database.mapper

import uk.gov.dluhc.printapi.database.entity.PrintRequestStatus
import uk.gov.dluhc.printapi.dto.PrintRequestStatusDto

class PrintRequestStatusDtoMapper {
    fun toPrintRequestStatusDto(status: PrintRequestStatus.Status): PrintRequestStatusDto {
        return PrintRequestStatusDto.valueOf(status.name)
    }
}
