package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.dto.GenerateAnonymousElectorDocumentDto
import uk.gov.dluhc.printapi.models.GenerateAnonymousElectorDocumentRequest

@Mapper(
    uses =
    [
        CertificateLanguageMapper::class,
        SourceTypeMapper::class,
        SupportingInformationFormatMapper::class
    ]
)
interface GenerateAnonymousElectorDocumentMapper {

    fun toGenerateAnonymousElectorDocumentDto(
        apiRequest: GenerateAnonymousElectorDocumentRequest,
        userId: String
    ): GenerateAnonymousElectorDocumentDto
}
