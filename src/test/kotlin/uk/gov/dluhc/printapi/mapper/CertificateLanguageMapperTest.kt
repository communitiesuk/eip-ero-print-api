package uk.gov.dluhc.printapi.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.printprovider.models.PrintRequest
import uk.gov.dluhc.printapi.database.entity.CertificateLanguage as CertificateLanguageEntity
import uk.gov.dluhc.printapi.dto.CertificateLanguage as CertificateLanguageDto
import uk.gov.dluhc.printapi.models.CertificateLanguage as CertificateLanguageApi

class CertificateLanguageMapperTest {

    private val mapper = CertificateLanguageMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "EN, EN",
            "CY, CY",
        ]
    )
    fun `should map CertificateLanguage entity enum to print request api enum`(
        source: CertificateLanguageEntity,
        expected: PrintRequest.CertificateLanguage
    ) {
        // Given

        // When
        val actual = mapper.mapEntityToPrintRequest(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "EN, EN",
            "CY, CY",
        ]
    )
    fun `should map CertificateLanguage api enum to DTO enum`(
        source: CertificateLanguageApi,
        expected: CertificateLanguageDto
    ) {
        // Given

        // When
        val actual = mapper.mapApiToDto(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "EN, EN",
            "CY, CY",
        ]
    )
    fun `should map CertificateLanguage DTO enum to CertificateLanguage Entity enum`(
        source: CertificateLanguageDto,
        expected: CertificateLanguageEntity
    ) {
        // Given

        // When
        val actual = mapper.mapDtoToEntity(source)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
