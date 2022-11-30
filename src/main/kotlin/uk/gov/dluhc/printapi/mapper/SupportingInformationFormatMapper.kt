package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.ValueMapping
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntityEnum
import uk.gov.dluhc.printapi.messaging.models.SupportingInformationFormat as SupportingInformationFormatModelEnum

@Mapper
interface SupportingInformationFormatMapper {

    /**
     * Maps a [SupportingInformationFormat] to a [PrintRequest.CertificateFormat]
     *
     * The name CertificateFormat is misleading and is tech debt. It's value is the format of the supporting information
     * that the elector wants with their posted certificate. It is not the format of the certificate itself.
     * Ideally we would like to rename the enum and it's corresponding field name to better reflect it's purpose
     * but this can only be done with agreement and coordination with the Print Provider as they will need to refactor
     * their code at the same time.
     */
    fun toPrintRequestApiEnum(supportingInformationFormat: SupportingInformationFormatEntityEnum): PrintRequest.CertificateFormat

    @ValueMapping(source = "LARGE_MINUS_PRINT", target = "LARGE_PRINT")
    @ValueMapping(source = "EASY_MINUS_READ", target = "EASY_READ")
    fun toPrintRequestEntityEnum(supportingInformationFormat: SupportingInformationFormatModelEnum): SupportingInformationFormatEntityEnum
}
