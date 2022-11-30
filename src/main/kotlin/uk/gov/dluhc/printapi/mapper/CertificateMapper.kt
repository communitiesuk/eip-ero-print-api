package uk.gov.dluhc.printapi.mapper

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.dto.IssuerDto
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(uses = [SourceTypeMapper::class, InstantMapper::class])
abstract class CertificateMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var printRequestMapper: PrintRequestMapper

    @Mapping(target = "vacNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(source = "issuer.englishContactDetails.name", target = "issuingAuthority")
    abstract fun toCertificate(
        message: SendApplicationToPrintMessage,
        issuer: IssuerDto
    ): Certificate

    @AfterMapping
    protected fun addPrintRequestToCertificate(
        message: SendApplicationToPrintMessage,
        issuer: IssuerDto,
        @MappingTarget certificate: Certificate
    ) {
        certificate.addPrintRequest(printRequestMapper.toPrintRequest(message, issuer))
    }
}
