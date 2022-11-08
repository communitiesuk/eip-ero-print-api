package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto

@Mapper
interface ElectoralRegistrationOfficeMapper {

    @Mapping(source = "name", target = "name")
    @Mapping(target = "phoneNumber", constant = "")
    @Mapping(target = "emailAddress", constant = "")
    @Mapping(target = "website", constant = "")
    @Mapping(target = "address.street", constant = "")
    @Mapping(target = "address.postcode", constant = "")
    fun map(dto: EroManagementApiEroDto): ElectoralRegistrationOffice
}
