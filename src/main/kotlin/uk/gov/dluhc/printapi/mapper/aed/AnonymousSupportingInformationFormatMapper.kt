package uk.gov.dluhc.printapi.mapper.aed

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntityEnum
import uk.gov.dluhc.printapi.dto.aed.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatDtoEnum
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatApiEnum

@Mapper
interface AnonymousSupportingInformationFormatMapper {

    fun mapApiToDto(apiFormat: AnonymousSupportingInformationFormatApiEnum): AnonymousSupportingInformationFormatDtoEnum

    @InheritInverseConfiguration
    fun mapDtoToApi(dtoFormat: AnonymousSupportingInformationFormatDtoEnum): AnonymousSupportingInformationFormatApiEnum

    fun mapDtoToEntity(dtoFormat: AnonymousSupportingInformationFormatDtoEnum): SupportingInformationFormatEntityEnum

    @InheritInverseConfiguration
    fun mapEntityToDto(entityFormat: SupportingInformationFormatEntityEnum): AnonymousSupportingInformationFormatDtoEnum
}
