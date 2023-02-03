package uk.gov.dluhc.printapi.service.temporarycertificate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto

internal class ExplainerPdfTemplateDetailsFactoryTest {

    companion object {
        // Example GSS Codes for different nations
        private const val GSS_CODE_ENGLAND = "E09000006" // England ERO has GSS Code starting with 'E'
        private const val GSS_CODE_WALES = "W06000023" // Wales ERO has GSS Code starting with 'W'
        private const val GSS_CODE_SCOTLAND = "S12000011" // Scotland ERO has GSS Code starting with 'S'
        private const val GSS_CODE_NORTHERN_IRELAND = "N07000001" // Northern Ireland ERO has GSS Code starting with 'N'

        // Template paths
        private const val ENGLISH_TEMPLATE_PATH = "/path/to/english-template.pdf"
        private const val WELSH_TEMPLATE_PATH = "/path/to/welsh-template.pdf"

        // English placeholders
        private const val ENGLISH_PLACEHOLDER_ERO_NAME = "eroNameEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_LINE1 = "eroAddressLine1EnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_LINE2 = "eroAddressLine2EnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_LINE3 = "eroAddressLine3EnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_LINE4 = "eroAddressLine4EnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_POSTCODE = "eroAddressPostcodeEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_EMAIL = "eroEmailAddressEnPlaceholder"
        private const val ENGLISH_PLACEHOLDER_ERO_PHONE = "eroPhoneNumberEnPlaceholder"

        // Welsh placeholders
        private const val WELSH_PLACEHOLDER_ERO_NAME = "eroNameCyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_LINE1 = "eroAddressLine1CyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_LINE2 = "eroAddressLine2CyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_LINE3 = "eroAddressLine3CyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_LINE4 = "eroAddressLine4CyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_POSTCODE = "eroAddressPostcodeCyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_EMAIL = "eroEmailAddressCyPlaceholder"
        private const val WELSH_PLACEHOLDER_ERO_PHONE = "eroPhoneNumberCyPlaceholder"
    }

    private val templateSelector = ExplainerPdfTemplateDetailsFactory(
        ENGLISH_TEMPLATE_PATH,
        ENGLISH_PLACEHOLDER_ERO_NAME,
        ENGLISH_PLACEHOLDER_ERO_LINE1,
        ENGLISH_PLACEHOLDER_ERO_LINE2,
        ENGLISH_PLACEHOLDER_ERO_LINE3,
        ENGLISH_PLACEHOLDER_ERO_LINE4,
        ENGLISH_PLACEHOLDER_ERO_POSTCODE,
        ENGLISH_PLACEHOLDER_ERO_EMAIL,
        ENGLISH_PLACEHOLDER_ERO_PHONE,
        WELSH_TEMPLATE_PATH,
        WELSH_PLACEHOLDER_ERO_NAME,
        WELSH_PLACEHOLDER_ERO_LINE1,
        WELSH_PLACEHOLDER_ERO_LINE2,
        WELSH_PLACEHOLDER_ERO_LINE3,
        WELSH_PLACEHOLDER_ERO_LINE4,
        WELSH_PLACEHOLDER_ERO_POSTCODE,
        WELSH_PLACEHOLDER_ERO_EMAIL,
        WELSH_PLACEHOLDER_ERO_PHONE,
    )

    @ParameterizedTest
    @CsvSource(value = [GSS_CODE_ENGLAND, GSS_CODE_SCOTLAND, GSS_CODE_NORTHERN_IRELAND])
    fun `should get template details when English template selected`(gssCode: String) {
        // Given
        val eroDto = buildEroDto()
        val expectedPlaceholders = with(eroDto.englishContactDetails) {
            mapOf(
                ENGLISH_PLACEHOLDER_ERO_NAME to name,
                ENGLISH_PLACEHOLDER_ERO_LINE1 to address.property.orEmpty(),
                ENGLISH_PLACEHOLDER_ERO_LINE2 to address.street,
                ENGLISH_PLACEHOLDER_ERO_LINE3 to address.town.orEmpty(),
                ENGLISH_PLACEHOLDER_ERO_LINE4 to address.area.orEmpty(),
                ENGLISH_PLACEHOLDER_ERO_POSTCODE to address.postcode,
                ENGLISH_PLACEHOLDER_ERO_EMAIL to emailAddress,
                ENGLISH_PLACEHOLDER_ERO_PHONE to phoneNumber,
            )
        }

        // When
        val actual = templateSelector.getTemplateDetails(gssCode, eroDto)

        // Then
        assertThat(actual.path).isEqualTo(ENGLISH_TEMPLATE_PATH)
        assertThat(actual.placeholders).isEqualTo(expectedPlaceholders)
    }

    @Test
    fun `should get template details when Welsh template selected`() {
        // Given
        val gssCode = GSS_CODE_WALES
        val eroDto = buildEroDto()
        val expectedPlaceholders = with(eroDto.welshContactDetails!!) {
            mapOf(
                WELSH_PLACEHOLDER_ERO_NAME to name,
                WELSH_PLACEHOLDER_ERO_LINE1 to address.property.orEmpty(),
                WELSH_PLACEHOLDER_ERO_LINE2 to address.street,
                WELSH_PLACEHOLDER_ERO_LINE3 to address.town.orEmpty(),
                WELSH_PLACEHOLDER_ERO_LINE4 to address.area.orEmpty(),
                WELSH_PLACEHOLDER_ERO_POSTCODE to address.postcode,
                WELSH_PLACEHOLDER_ERO_EMAIL to emailAddress,
                WELSH_PLACEHOLDER_ERO_PHONE to phoneNumber,
            )
        }

        // When
        val actual = templateSelector.getTemplateDetails(gssCode, eroDto)

        // Then
        assertThat(actual.path).isEqualTo(WELSH_TEMPLATE_PATH)
        assertThat(actual.placeholders).isEqualTo(expectedPlaceholders)
    }
}
