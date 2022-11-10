package uk.gov.dluhc.printapi.rds.mapper

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.dto.EroManagementApiEroDto
import uk.gov.dluhc.printapi.mapper.SourceTypeMapper
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.rds.entity.Certificate
import uk.gov.dluhc.printapi.rds.entity.PrintRequest
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(uses = [SourceTypeMapper::class, InstantMapper::class])
abstract class CertificateMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var printRequestMapper: PrintRequestMapper

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vacNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(source = "localAuthority", target = "issuingAuthority")
    abstract fun toCertificate(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto,
        localAuthority: String
    ): Certificate

    @AfterMapping
    protected fun addPrintRequestToCertificate(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto,
        @MappingTarget certificate: Certificate
    ) {
        certificate.addPrintRequest(printRequestMapper.toPrintRequest(message, ero))
    }
}
