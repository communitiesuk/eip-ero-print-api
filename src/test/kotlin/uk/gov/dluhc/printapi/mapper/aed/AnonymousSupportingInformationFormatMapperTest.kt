package uk.gov.dluhc.printapi.mapper.aed

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.dluhc.printapi.database.entity.SupportingInformationFormat as SupportingInformationFormatEntityEnum
import uk.gov.dluhc.printapi.dto.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatDtoEnum
import uk.gov.dluhc.printapi.models.AnonymousSupportingInformationFormat as AnonymousSupportingInformationFormatApiEnum

class AnonymousSupportingInformationFormatMapperTest {

    private val mapper = AnonymousSupportingInformationFormatMapperImpl()

    @ParameterizedTest
    @CsvSource(
        value = [
            "STANDARD, STANDARD",
            "BRAILLE, BRAILLE",
            "LARGE_MINUS_PRINT, LARGE_PRINT",
            "EASY_MINUS_READ, EASY_READ",
        ]
    )
    fun `should map AnonymousSupportingInformationFormat API enum to DTO enum`(
        aedSupportingFormatApi: AnonymousSupportingInformationFormatApiEnum,
        expected: AnonymousSupportingInformationFormatDtoEnum
    ) {
        // Given
        // When
        val actual = mapper.mapApiToDto(aedSupportingFormatApi)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "STANDARD, STANDARD",
            "BRAILLE, BRAILLE",
            "LARGE_PRINT, LARGE_MINUS_PRINT",
            "EASY_READ, EASY_MINUS_READ",
        ]
    )
    fun `should map AnonymousSupportingInformationFormat DTO enum to API enum`(
        aedSupportingFormatDto: AnonymousSupportingInformationFormatDtoEnum,
        expected: AnonymousSupportingInformationFormatApiEnum
    ) {
        // Given
        // When
        val actual = mapper.mapDtoToApi(aedSupportingFormatDto)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "STANDARD, STANDARD",
            "BRAILLE, BRAILLE",
            "LARGE_PRINT, LARGE_PRINT",
            "EASY_READ, EASY_READ",
        ]
    )
    fun `should map AnonymousSupportingInformationFormat DTO enum to entity enum`(
        aedSupportingFormatDto: AnonymousSupportingInformationFormatDtoEnum,
        expected: SupportingInformationFormatEntityEnum
    ) {
        // Given
        // When
        val actual = mapper.mapDtoToEntity(aedSupportingFormatDto)

        // Then
        assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "STANDARD, STANDARD",
            "BRAILLE, BRAILLE",
            "LARGE_PRINT, LARGE_PRINT",
            "EASY_READ, EASY_READ",
        ]
    )
    fun `should map AnonymousSupportingInformationFormat entity enum to DTO enum`(
        aedSupportingFormatEntity: SupportingInformationFormatEntityEnum,
        expected: AnonymousSupportingInformationFormatDtoEnum
    ) {
        // Given
        // When
        val actual = mapper.mapEntityToDto(aedSupportingFormatEntity)

        // Then
        assertThat(actual).isEqualTo(expected)
    }
}
