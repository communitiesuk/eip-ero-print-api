package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto

@Mapper
interface ElectoralRegistrationOfficeMapper {

    fun map(dto: EroContactDetailsDto): ElectoralRegistrationOffice
}
