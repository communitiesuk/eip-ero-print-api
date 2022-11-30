package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest

@Mapper
interface CertificateLanguageMapper {

    fun toPrintRequestApiEnum(certificateLanguage: CertificateLanguage): PrintRequest.CertificateLanguage
}
