package uk.gov.dluhc.printapi.rds.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.rds.entity.ElectoralRegistrationOffice

@Mapper
interface RdsElectoralRegistrationOfficeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "phoneNumber", constant = "")
    @Mapping(target = "emailAddress", constant = "")
    @Mapping(target = "website", constant = "")
    @Mapping(target = "address.street", constant = "")
    @Mapping(target = "address.postcode", constant = "")
    fun map(dto: EroManagementApiEroDto): ElectoralRegistrationOffice
}
