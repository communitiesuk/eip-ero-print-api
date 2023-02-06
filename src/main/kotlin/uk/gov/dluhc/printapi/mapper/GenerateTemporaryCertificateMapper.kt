package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.models.GenerateTemporaryCertificateRequest

@Mapper(uses = [CertificateLanguageMapper::class, SourceTypeMapper::class])
interface GenerateTemporaryCertificateMapper {

    fun toGenerateTemporaryCertificateDto(apiRequest: GenerateTemporaryCertificateRequest, userId: String): GenerateTemporaryCertificateDto
}
