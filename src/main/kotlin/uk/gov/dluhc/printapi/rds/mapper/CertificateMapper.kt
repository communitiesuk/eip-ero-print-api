package uk.gov.dluhc.printapi.rds.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
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
    @Mapping(target = "printRequests", expression = "java( toPrintRequestList(message, ero) )")
    abstract fun toCertificate(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto,
        localAuthority: String
    ): Certificate

    protected fun toPrintRequestList(
        message: SendApplicationToPrintMessage,
        ero: EroManagementApiEroDto
    ): MutableList<PrintRequest> {
        return mutableListOf(printRequestMapper.toPrintRequest(message, ero))
    }
}
