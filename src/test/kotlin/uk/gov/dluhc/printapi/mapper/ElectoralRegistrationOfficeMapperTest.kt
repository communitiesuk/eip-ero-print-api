package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.dluhc.printapi.messaging.models.CertificateLanguage
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aWelshEroContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.dto.anEnglishEroContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.dto.toElectoralRegistrationOffice

private const val ELECTORAL_REGISTRATION_OFFICER_EN = "Electoral Registration Officer"
private const val ELECTORAL_REGISTRATION_OFFICER_CY = "Swyddog Cofrestru Etholiadol"

class ElectoralRegistrationOfficeMapperTest {
    val mapper = ElectoralRegistrationOfficeMapperImpl()

    @Test
    fun `should map given English contact details`() {
        // Given
        val contactDetails = anEnglishEroContactDetails()
        val expected = contactDetails.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_EN)

        // When
        val actual = mapper.toElectoralRegistrationOffice(contactDetails, CertificateLanguage.EN)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map given Welsh contact details`() {
        // Given
        val contactDetails = aWelshEroContactDetails()
        val expected = contactDetails.toElectoralRegistrationOffice(ELECTORAL_REGISTRATION_OFFICER_CY)

        // When
        val actual = mapper.toElectoralRegistrationOffice(contactDetails, CertificateLanguage.CY)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(CertificateLanguage::class)
    fun `should map given null contact details`(certificateLanguage: CertificateLanguage) {
        // Given
        val contactDetails = null
        val expected = null

        // When
        val actual = mapper.toElectoralRegistrationOffice(contactDetails, certificateLanguage)

        // Then
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected)
    }
}
