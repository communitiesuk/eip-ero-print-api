package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest

class CertificateLanguageMapperTest {

    private val mapper = CertificateLanguageMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "EN, EN",
            "CY, CY",
        ]
    )
    fun `should map CertificateLanguage to print request api enum`(
        source: CertificateLanguage,
        expected: PrintRequest.CertificateLanguage
    ) {
        // Given

        // When
        val actual = mapper.toPrintRequestApiEnum(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
