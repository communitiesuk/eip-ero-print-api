package uk.gov.dluhc.printapi.mapper

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import uk.gov.dluhc.printapi.database.entity.ElectoralRegistrationOffice
import uk.gov.dluhc.printapi.dto.EroContactDetailsDto
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage.CY
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage.EN

private const val ELECTORAL_REGISTRATION_OFFICER_EN = "Electoral Registration Officer"
private const val ELECTORAL_REGISTRATION_OFFICER_CY = "Swyddog Cofrestru Etholiadol"

@Mapper
abstract class ElectoralRegistrationOfficeMapper {
    fun toElectoralRegistrationOffice(
        eroContactDetails: EroContactDetailsDto?,
        language: CertificateLanguage
    ): ElectoralRegistrationOffice? {
        if (eroContactDetails == null) {
            return null
        }

        return toElectoralRegistrationOfficeFromNotNullContactDetails(eroContactDetails, language)
    }

    @Mapping(target = "name", expression = "java( getName(language) )")
    protected abstract fun toElectoralRegistrationOfficeFromNotNullContactDetails(
        eroContactDetails: EroContactDetailsDto,
        language: CertificateLanguage
    ): ElectoralRegistrationOffice

    protected fun getName(language: CertificateLanguage): String {
        return when (language) {
            CY -> ELECTORAL_REGISTRATION_OFFICER_CY
            EN -> ELECTORAL_REGISTRATION_OFFICER_EN
        }
    }
}
