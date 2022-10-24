package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.eromanagementapi.models.ElectoralRegistrationOfficeResponse
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto

@Mapper
interface EroManagementApiEroDtoMapper {
    fun toEroManagementApiEroDto(eroResponse: ElectoralRegistrationOfficeResponse): EroManagementApiEroDto
}
