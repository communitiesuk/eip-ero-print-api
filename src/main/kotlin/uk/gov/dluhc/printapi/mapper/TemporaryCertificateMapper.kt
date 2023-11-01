package uk.gov.dluhc.printapi.mapper

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificate
import uk.gov.dluhc.printapi.database.entity.TemporaryCertificateStatus
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.dto.GenerateTemporaryCertificateDto
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(uses = [SourceTypeMapper::class, CertificateLanguageMapper::class])
abstract class TemporaryCertificateMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Mapping(target = "photoLocationArn", source = "certificateRequest.photoLocation")
    @Mapping(target = "issuingAuthority", source = "ero.englishContactDetails.nameVac")
    @Mapping(target = "issuingAuthorityCy", source = "ero.welshContactDetails.nameVac")
    @Mapping(target = "certificateNumber", expression = "java( idFactory.vacNumber() )")
    abstract fun toTemporaryCertificate(
        certificateRequest: GenerateTemporaryCertificateDto,
        ero: EroDto,
        certificateTemplateFilename: String
    ): TemporaryCertificate

    @AfterMapping
    protected fun addStatusToTemporaryCertificate(
        certificateRequest: GenerateTemporaryCertificateDto,
        @MappingTarget temporaryCertificate: TemporaryCertificate
    ) {
        temporaryCertificate.addTemporaryCertificateStatus(
            TemporaryCertificateStatus()
                .apply {
                    status = TemporaryCertificateStatus.Status.GENERATED
                    userId = certificateRequest.userId
                }
        )
    }
}
