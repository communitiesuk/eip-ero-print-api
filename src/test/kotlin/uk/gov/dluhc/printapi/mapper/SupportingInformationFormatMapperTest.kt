package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest

class SupportingInformationFormatMapperTest {

    private val mapper = SupportingInformationFormatMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "STANDARD, STANDARD",
            "BRAILLE, BRAILLE",
            "EASY_READ, EASY_READ",
            "LARGE_PRINT, LARGE_PRINT",
        ]
    )
    fun `should map SupportingInformationFormat to print request api enum`(
        source: SupportingInformationFormat,
        expected: PrintRequest.CertificateFormat
    ) {
        // Given

        // When
        val actual = mapper.toPrintRequestApiEnum(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
