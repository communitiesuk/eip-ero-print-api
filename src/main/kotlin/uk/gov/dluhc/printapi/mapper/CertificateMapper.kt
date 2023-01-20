package uk.gov.dluhc.printapi.mapper

import org.mapstruct.AfterMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.dluhc.printapi.database.entity.Certificate
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.dto.EroDto
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.SendApplicationToPrintMessage
import uk.gov.dluhc.printapi.service.IdFactory

@Mapper(uses = [SourceTypeMapper::class, InstantMapper::class])
abstract class CertificateMapper {

    @Autowired
    protected lateinit var idFactory: IdFactory

    @Autowired
    protected lateinit var printRequestMapper: PrintRequestMapper

    @Mapping(target = "vacNumber", expression = "java( idFactory.vacNumber() )")
    @Mapping(source = "ero.englishContactDetails.name", target = "issuingAuthority")
    @Mapping(expression = "java( welshIssuingAuthorityName(message, ero.getWelshContactDetails()) )", target = "issuingAuthorityCy")
    abstract fun toCertificate(
        message: SendApplicationToPrintMessage,
        ero: EroDto
    ): Certificate

    @AfterMapping
    protected fun addPrintRequestToCertificate(
        message: SendApplicationToPrintMessage,
        ero: EroDto,
        @MappingTarget certificate: Certificate
    ) {
        certificate.addPrintRequest(printRequestMapper.toPrintRequest(message, ero))
    }

    protected fun welshIssuingAuthorityName(message: SendApplicationToPrintMessage, welshContactDetails: EroContactDetailsDto?): String? {
        return if (message.certificateLanguage == CertificateLanguage.CY && welshContactDetails != null) {
            welshContactDetails.name
        } else {
            null
        }
    }
}
