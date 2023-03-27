package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.dto.CertificateLanguage as CertificateLanguageDto
import uk.gov.dluhc.printapi.models.CertificateLanguage as CertificateLanguageApi

@Mapper
interface CertificateLanguageMapper {

    fun mapEntityToPrintRequest(entityLanguage: CertificateLanguageEntity): PrintRequest.CertificateLanguage

    fun mapApiToDto(apiLanguage: CertificateLanguageApi): CertificateLanguageDto

    fun mapEntityToDto(entityLanguage: CertificateLanguageEntity): CertificateLanguageDto

    fun mapDtoToApi(dtoLanguage: CertificateLanguageDto): CertificateLanguageApi

    fun mapDtoToEntity(dtoLanguage: CertificateLanguageDto): CertificateLanguageEntity
}
