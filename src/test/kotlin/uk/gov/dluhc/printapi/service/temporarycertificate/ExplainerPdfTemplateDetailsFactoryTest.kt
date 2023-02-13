package uk.gov.dluhc.printapi.service.temporarycertificate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.config.TemporaryCertificateExplainerPdfTemplateProperties
import uk.gov.dluhc.printapi.config.TemporaryCertificateExplainerPdfTemplateProperties.English
import uk.gov.dluhc.printapi.config.TemporaryCertificateExplainerPdfTemplateProperties.Placeholder
import uk.gov.dluhc.printapi.config.TemporaryCertificateExplainerPdfTemplateProperties.Welsh
import uk.gov.dluhc.printapi.dto.AddressDto
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_ENGLAND
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_NORTHERN_IRELAND
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_SCOTLAND
import uk.gov.dluhc.printapi.service.GssCodeInterpreterKtTest.Companion.GSS_CODE_WALES
import uk.gov.dluhc.printapi.testsupport.testdata.dto.aWelshEroContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.dto.anEnglishEroContactDetails
import uk.gov.dluhc.printapi.testsupport.testdata.dto.buildEroDto

internal class ExplainerPdfTemplateDetailsFactoryTest {

    companion object {
        // Template paths
        private const val ENGLISH_TEMPLATE_PATH = "/path/to/english-template.pdf"
        private const val WELSH_TEMPLATE_PATH = "/path/to/welsh-template.pdf"

        // English placeholders
        private const val ENGLISH_CONTACT_DETAIL_1 = "eroNameEnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_2 = "eroAddressLine1EnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_3 = "eroAddressLine2EnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_4 = "eroAddressLine3EnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_5 = "eroAddressLine4EnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_6 = "eroAddressPostcodeEnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_7 = "eroEmailAddressEnPlaceholder"
        private const val ENGLISH_CONTACT_DETAIL_8 = "eroPhoneNumberEnPlaceholder"

        // Welsh placeholders
        private const val WELSH_CONTACT_DETAIL_1 = "eroNameCyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_2 = "eroAddressLine1CyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_3 = "eroAddressLine2CyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_4 = "eroAddressLine3CyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_5 = "eroAddressLine4CyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_6 = "eroAddressPostcodeCyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_7 = "eroEmailAddressCyPlaceholder"
        private const val WELSH_CONTACT_DETAIL_8 = "eroPhoneNumberCyPlaceholder"
    }

    private val templateSelector = ExplainerPdfTemplateDetailsFactory(
        TemporaryCertificateExplainerPdfTemplateProperties(
            English(
                ENGLISH_TEMPLATE_PATH,
                Placeholder(
                    ENGLISH_CONTACT_DETAIL_1,
                    ENGLISH_CONTACT_DETAIL_2,
                    ENGLISH_CONTACT_DETAIL_3,
                    ENGLISH_CONTACT_DETAIL_4,
                    ENGLISH_CONTACT_DETAIL_5,
                    ENGLISH_CONTACT_DETAIL_6,
                    ENGLISH_CONTACT_DETAIL_7,
                    ENGLISH_CONTACT_DETAIL_8
                )
            ),
            Welsh(
                WELSH_TEMPLATE_PATH,
                Placeholder(
                    WELSH_CONTACT_DETAIL_1,
                    WELSH_CONTACT_DETAIL_2,
                    WELSH_CONTACT_DETAIL_3,
                    WELSH_CONTACT_DETAIL_4,
                    WELSH_CONTACT_DETAIL_5,
                    WELSH_CONTACT_DETAIL_6,
                    WELSH_CONTACT_DETAIL_7,
                    WELSH_CONTACT_DETAIL_8
                )
            )
        )
    )

    @ParameterizedTest
    @CsvSource(value = [GSS_CODE_ENGLAND, GSS_CODE_SCOTLAND, GSS_CODE_NORTHERN_IRELAND])
    fun `should get template details when English template selected`(gssCode: String) {
        // Given
        val eroDto = buildEroDto(
            englishContactDetails = anEnglishEroContactDetails(
                address = AddressDto(
                    property = "Gwynedd Council Headquarters",
                    street = "Shirehall Street",
                    town = "Caernarfon",
                    area = "Gwynedd",
                    postcode = "LL55 1SH",
                )
            )
        )
        val expectedPlaceholders = with(eroDto.englishContactDetails) {
            mapOf(
                ENGLISH_CONTACT_DETAIL_1 to name,
                ENGLISH_CONTACT_DETAIL_2 to address.property,
                ENGLISH_CONTACT_DETAIL_3 to address.street,
                ENGLISH_CONTACT_DETAIL_4 to address.town,
                ENGLISH_CONTACT_DETAIL_5 to address.area,
                ENGLISH_CONTACT_DETAIL_6 to address.postcode,
                ENGLISH_CONTACT_DETAIL_7 to emailAddress,
                ENGLISH_CONTACT_DETAIL_8 to phoneNumber,
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
        val eroDto = buildEroDto(
            welshContactDetails = aWelshEroContactDetails(
                address = AddressDto(
                    property = "Pencadlys Cyngor Gwynedd",
                    street = "Stryd y Jêl",
                    town = "Caernarfon",
                    area = "Gwynedd",
                    postcode = "LL55 1SH",
                )
            )
        )
        val expectedPlaceholders = with(eroDto.welshContactDetails!!) {
            mapOf(
                WELSH_CONTACT_DETAIL_1 to name,
                WELSH_CONTACT_DETAIL_2 to address.property,
                WELSH_CONTACT_DETAIL_3 to address.street,
                WELSH_CONTACT_DETAIL_4 to address.town,
                WELSH_CONTACT_DETAIL_5 to address.area,
                WELSH_CONTACT_DETAIL_6 to address.postcode,
                WELSH_CONTACT_DETAIL_7 to emailAddress,
                WELSH_CONTACT_DETAIL_8 to phoneNumber,
            )
        }

        // When
        val actual = templateSelector.getTemplateDetails(gssCode, eroDto)

        // Then
        assertThat(actual.path).isEqualTo(WELSH_TEMPLATE_PATH)
        assertThat(actual.placeholders).isEqualTo(expectedPlaceholders)
    }

    @Test
    fun `should get template details when optional properties are not available`() {
        // Given
        val gssCode = GSS_CODE_WALES
        val eroDto = buildEroDto(
            welshContactDetails = aWelshEroContactDetails(
                address = AddressDto(
                    property = null,
                    street = "Stryd y Jêl",
                    town = null,
                    area = null,
                    postcode = "LL55 1SH",
                )
            )
        )
        val expectedPlaceholders = with(eroDto.welshContactDetails!!) {
            mapOf(
                WELSH_CONTACT_DETAIL_1 to name,
                WELSH_CONTACT_DETAIL_2 to address.street,
                WELSH_CONTACT_DETAIL_3 to address.postcode,
                WELSH_CONTACT_DETAIL_4 to emailAddress,
                WELSH_CONTACT_DETAIL_5 to phoneNumber,
                WELSH_CONTACT_DETAIL_6 to "",
                WELSH_CONTACT_DETAIL_7 to "",
                WELSH_CONTACT_DETAIL_8 to "",
            )
        }

        // When
        val actual = templateSelector.getTemplateDetails(gssCode, eroDto)

        // Then
        assertThat(actual.path).isEqualTo(WELSH_TEMPLATE_PATH)
        assertThat(actual.placeholders).isEqualTo(expectedPlaceholders)
    }
}
