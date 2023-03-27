package uk.gov.dluhc.printapi.mapper

import org.mapstruct.InheritInverseConfiguration
import org.mapstruct.Mapper
import org.mapstruct.MappingConstants
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntityEnum
import uk.gov.dluhc.printapi.dto.SupportingInformationFormat as SupportingInformationFormatDtoEnum
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat as SupportingInformationFormatSqsEnum
import uk.gov.dluhc.printapi.models.SupportingInformationFormat as SupportingInformationFormatApiEnum

@Mapper
interface SupportingInformationFormatMapper {

    /**
     * Maps a [SupportingInformationFormatEntityEnum] to a [PrintRequest.CertificateFormat]
     *
     * The name CertificateFormat is misleading and is tech debt. Its value is the format of the supporting information
     * that the elector wants with their posted certificate. It is not the format of the certificate itself.
     * Ideally we would like to rename the enum and its corresponding field name to better reflect its purpose
     * but this can only be done with agreement and coordination with the Print Provider as they will need to refactor
     * their code at the same time.
     */
    fun toPrintRequestApiEnum(entityFormat: SupportingInformationFormatEntityEnum): PrintRequest.CertificateFormat

    @ValueMapping(source = "LARGE_MINUS_PRINT", target = "LARGE_PRINT")
    @ValueMapping(source = "EASY_MINUS_READ", target = "EASY_READ")
    fun toPrintRequestEntityEnum(sqsFormat: SupportingInformationFormatSqsEnum): SupportingInformationFormatEntityEnum

    fun mapDtoToEntity(dtoFormat: SupportingInformationFormatDtoEnum): SupportingInformationFormatEntityEnum

    @ValueMapping(source = "STANDARD", target = "STANDARD")
    @ValueMapping(source = MappingConstants.ANY_UNMAPPED, target = MappingConstants.THROW_EXCEPTION)
    fun mapEntityToDto(entityFormat: SupportingInformationFormatEntityEnum): SupportingInformationFormatDtoEnum

    fun mapApiToDto(apiFormat: SupportingInformationFormatApiEnum): SupportingInformationFormatDtoEnum

    @InheritInverseConfiguration
    fun mapDtoToApi(dtoFormat: SupportingInformationFormatDtoEnum): SupportingInformationFormatApiEnum
}
